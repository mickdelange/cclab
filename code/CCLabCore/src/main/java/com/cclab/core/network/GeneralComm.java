package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Abstract thread capable of communicating with external processes
 * <p/>
 * It holds standard implementation aspects of a Java NIO client-server
 * communication pattern. All registered channels are monitored for new events.
 * Messages can be sent by adding them to the outgoing queue. The constructor
 * expects a listening port, the signature for coordination messages and a
 * reference to an interpreter to which incoming messages can be reported.
 * <p/>
 * Created on 10/20/14 for CCLabCore.
 *
 * @author an3m0na
 */
public abstract class GeneralComm extends Thread {
    static final int BUF_SIZE = 8192;
    public static final int DEFAULT_PORT = 9026;
    private static final int TIMEOUT = 10000;

    String myName;
    int port;
    Selector selector;

    boolean shouldExit = false;
    CommInterpreter interpreter = null;
    ConcurrentHashMap<SocketChannel, BigDataReceiver> bigDataReceivers = null;
    ConcurrentHashMap<SocketChannel, BigDataSender> bigDataSenders = null;
    ConcurrentHashMap<SocketChannel, ConcurrentLinkedQueue<Message>> outgoingQueues;

    public GeneralComm(int port, String myName, CommInterpreter interpreter) {
        this.port = port;
        this.myName = myName;
        this.interpreter = interpreter;

        bigDataReceivers = new ConcurrentHashMap<SocketChannel, BigDataReceiver>();
        bigDataSenders = new ConcurrentHashMap<SocketChannel, BigDataSender>();
        outgoingQueues = new ConcurrentHashMap<SocketChannel, ConcurrentLinkedQueue<Message>>();
    }

    @Override
    public void run() {
        try {
            // main loop
            while (true) {
                if (shouldExit)
                    break;
                checkOutgoing();
                // wait for something to happen
                selector.select(TIMEOUT);

                // iterate over the events
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    // get current event and REMOVE it from the list!!!
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error forced communicator to shut down", e);
        } finally {
            cleanup();
            NodeLogger.get().warn("Communicator disconnected");
            interpreter.communicatorDown(this);
        }
    }

    void addMessageToOutgoing(Message message, SocketChannel channel) {
        outgoingQueues.get(channel).add(message);
        selector.wakeup();
    }

    void cancelConnection(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        NodeLogger.get().warn("Connection closed for " + channel.socket().getRemoteSocketAddress());

        key.cancel();
        key.channel().close();
    }

    void cleanup() {
        if (selector != null)
            try {
                selector.close();
            } catch (IOException e) {
                NodeLogger.get().warn("Error closing selector", e);
            }
    }

    void checkOutgoing() {
        for (Map.Entry<SocketChannel, BigDataSender> e : bigDataSenders.entrySet()) {
            e.getKey().keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        }
        for (Map.Entry<SocketChannel, ConcurrentLinkedQueue<Message>> e : outgoingQueues.entrySet()) {
            if (!e.getValue().isEmpty()) {
                e.getKey().keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        // write in same thread
        BigDataSender sender = bigDataSenders.get(channel);
        if (sender != null) {
            sender.doSend();
            return;
        }
        Message message = outgoingQueues.get(channel).poll();
        if (message == null)
            return;
        if (message.getType() == Message.Type.NEWTASK.getCode() ||
                message.getType() == Message.Type.FINISHED.getCode()) {
            byte[] data = (byte[]) message.getData();
            message.setData(data.length);
            bigDataSenders.put(channel, new BigDataSender(key, data, message, this));
        }
        new Transceiver(key, message, this).run();
    }

    public void quit() {
        shouldExit = true;
        selector.wakeup();
    }

    void handleMessage(Message message, SocketChannel channel) {
        if (message.getType() == Message.Type.NEWTASK.getCode() ||
                message.getType() == Message.Type.FINISHED.getCode()) {
            NodeLogger.get().debug("Waiting for " + message.getData() + " bytes from " + message.getOwner());
            bigDataReceivers.put(channel, new BigDataReceiver(channel.keyFor(selector), this, message));
            return;
        }
        interpreter.processMessage(message);
    }

    void finishedSending(Message message, SocketChannel channel) {
        NodeLogger.get().debug("Resuming normal send mode for  " + message.getOwner());
        bigDataSenders.remove(channel);
    }

    void handleBigMessage(Message message, SocketChannel channel) {
        NodeLogger.get().debug("Resuming normal receive mode for  " + message.getOwner());
        bigDataReceivers.remove(channel);
        interpreter.processMessage(message);
    }

    abstract void accept(SelectionKey key) throws IOException;

    abstract void read(SelectionKey key) throws IOException;

    abstract void connect(SelectionKey key) throws IOException;

}
