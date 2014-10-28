package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ane on 10/20/14.
 */
public abstract class GeneralComm extends Thread {
    int port;
    Selector selector;
    static final int BUF_SIZE = 8192;
    public static final int DEFAULT_PORT = 9026;
    private boolean shouldExit = false;
    MessageInterpreter interpreter = null;
    ConcurrentHashMap<SocketChannel, ConcurrentHashMap<Integer, byte[]>> messageParts = null;

    public GeneralComm(int port, MessageInterpreter interpreter) {
        this.port = port;
        this.interpreter = interpreter;
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
                selector.select();
                // iterate over the events
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    // get current event and REMOVE it from the list!!!
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }

        } catch (IOException e) {
            NodeLogger.get().error(e.getMessage(), e);

        } finally {
            // cleanup
            cleanup();
            if (selector != null)
                try {
                    selector.close();
                } catch (IOException e) {
                }
        }
    }

    void writeFromQueue(SelectionKey key, ConcurrentLinkedQueue<Message> queue) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = null;
        if (queue != null && !queue.isEmpty()) {
            try {
                Message message = queue.poll();
                byte[] data = message.toBytes();
                NodeLogger.get().info("Sending " + message);
                System.out.println("Data length " + data.length);


                int chunks = data.length / BUF_SIZE;
                if (data.length % BUF_SIZE > 0)
                    chunks++;
                int crt = 0;
                while (crt < chunks) {
                    buf = ByteBuffer.allocateDirect(data.length + 12);
                    int size = Math.min(data.length - crt*BUF_SIZE, BUF_SIZE);
                    buf.putInt(chunks);
                    buf.putInt(crt);
                    buf.putInt(size);
                    byte[] part = Arrays.copyOfRange(data, crt*BUF_SIZE, crt*BUF_SIZE + size);
                    buf.put(part);
                    buf.flip();
                    while (buf.hasRemaining()) {
                        channel.write(buf);
                    }
                    System.out.println("Sent " + (crt*BUF_SIZE+size) + " of " + data.length);
                    crt++;
                }

            } catch (IOException e) {
                if (buf != null)
                    buf.clear();
                channel.close();
                return;
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    abstract void checkOutgoing();

    abstract void accept(SelectionKey key) throws IOException;

    abstract void write(SelectionKey key) throws IOException;

    abstract void read(SelectionKey key) throws IOException;

    abstract void cleanup();

    abstract void initialize() throws IOException;

    public void quit() {
        shouldExit = true;
        selector.wakeup();
    }

    abstract void checkIfNew(String clientName, SocketChannel socketChannel);

    abstract void disconnectClient(SocketChannel socketChannel);

    void handlePartialMessage(int total, int current , byte[] data, SocketChannel channel) throws IOException {
        ConcurrentHashMap<Integer, byte[]> channelParts = messageParts.get(channel);
        if (channelParts == null) {
            channelParts = new ConcurrentHashMap<Integer, byte[]>();
            messageParts.put(channel, channelParts);
        }
        channelParts.put(current, data);

        System.out.println("Received part " + current + " now " + channelParts.size());
        if (channelParts.size() == total) {
            handleMessage(Message.getFromParts(channelParts), channel);
            messageParts.put(channel, new ConcurrentHashMap<Integer, byte[]>());
        }
    }

    void handleMessage(Message message, SocketChannel channel) throws IOException {
        finishedReading(channel.keyFor(selector));
        //TODO interpret message
        System.out.println("Got it");
        checkIfNew(message.getOwner(), channel);
        NodeLogger.get().info("Received " + message);
        interpreter.processMessage(message);
    }

    abstract void finishedReading(SelectionKey key);
}
