package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ane on 10/19/14.
 */
public class ClientComm extends GeneralComm {

    private String masterIP;
    SocketChannel mainChannel = null;


    public ClientComm(String masterIP, int port, String myName, MessageInterpreter interpreter) throws IOException {
        super(port, myName, interpreter);

        this.masterIP = masterIP;

        initialize();
    }

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

        outgoingQueues.clear();
        outgoingQueues.put(mainChannel, new ConcurrentLinkedQueue<Message>());
        addMessageToOutgoing(new Message(Message.Type.PING, myName), mainChannel);
    }

    public void addMessageToOutgoing(Message message) {
        addMessageToOutgoing(message, mainChannel);
    }

    @Override
    void accept(SelectionKey key) throws IOException {
        NodeLogger.get().error("Cannot handle accept");
    }

    @Override
    void read(SelectionKey key) throws IOException {
        // read message in same thread
        new Transceiver(key, null, this).run();
    }

    @Override
    void cleanup() {
        if (mainChannel != null) {
            try {
                mainChannel.close();
            } catch (IOException e) {
                NodeLogger.get().warn("Error cleaning up communicator ", e);
            }
        }
        super.cleanup();
    }

    @Override
    void handleMessage(Message message, SocketChannel channel) throws IOException {
        interpreter.processMessage(message);
    }

    @Override
    void cancelConnection(SelectionKey key) throws IOException {
//        super.cancelConnection(key);
//        try {
//            cleanup();
//            initialize();
//        } catch (Exception e) {
//            interpreter.communicatorDown(this);
//            selector.wakeup();
//        }
        shouldExit = true;
        interpreter.communicatorDown(this);

    }
}
