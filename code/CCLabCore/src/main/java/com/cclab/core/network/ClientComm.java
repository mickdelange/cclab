package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ane on 10/19/14.
 */
public class ClientComm extends GeneralComm {

    private ConcurrentLinkedQueue<Message> outgoingQueue;
    private String masterIP;
    SocketChannel mainChannel = null;
    private String myName;

    public ClientComm(String masterIP, int port, String myName, MessageInterpreter interpreter) throws IOException {
        super(port, interpreter);
        this.myName = myName;
        this.masterIP = masterIP;
        outgoingQueue = new ConcurrentLinkedQueue<Message>();
        messageParts = new ConcurrentHashMap<SocketChannel, ConcurrentHashMap<Integer, byte[]>>();

        initialize();

    }

    @Override
    void initialize() throws IOException {
        NodeLogger.get().info("ClientComm communicator is now online");
        NodeLogger.get().info("Connecting to " + masterIP + ":" + port);

        mainChannel = SocketChannel.open();
        mainChannel.configureBlocking(false);
        mainChannel.connect(new InetSocketAddress(masterIP, port));
        mainChannel.finishConnect();

        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);
        selector = Selector.open();
        mainChannel.register(selector, SelectionKey.OP_READ, buf);
        messageParts.put(mainChannel, new ConcurrentHashMap<Integer, byte[]>());
        outgoingQueue.add(new Message(Message.Type.PING, myName));
    }

    @Override
    void checkOutgoing() {
        if (!outgoingQueue.isEmpty()) {
            mainChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        }
    }

    @Override
    void accept(SelectionKey key) throws IOException {
        NodeLogger.get().error("Cannot handle accept");
    }

    @Override
    void write(SelectionKey key) throws IOException {
        writeFromQueue(key, outgoingQueue);
    }

    @Override
    void read(SelectionKey key) throws IOException {
        new Thread(new ClientReceiver(key, this)).start();
    }

    @Override
    void cleanup() {
        if (mainChannel != null)
            try {
                mainChannel.close();
            } catch (IOException e) {
            }
    }

    public void addMessageToQueue(Message message) {
        outgoingQueue.add(message);
        selector.wakeup();
    }

    @Override
    public void checkIfNew(String clientName, SocketChannel socketChannel) {
    }

    @Override
    public void disconnectClient(SocketChannel socketChannel) {
    }

    @Override
    void finishedReading(SelectionKey key) {

    }
}
