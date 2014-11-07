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
 * Extension of the standard communicator to act as a network client.
 * <p/>
 * Apart from the parameters of a GeneralComm, the constructor expects the IP
 * address and port of the process it will connect to. On connection to the
 * server, it sends a default PING message to notify server of its name. All
 * messages are handled in a serial fashion. If the connection to the server is
 * lost, the communicator is forced to shutdown.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class ClientComm extends GeneralComm {

    private String masterIP;
    SocketChannel mainChannel = null;

    public ClientComm(String masterIP, int port, String myName, CommInterpreter interpreter) throws IOException {
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

        selector = Selector.open();
        mainChannel.register(selector, SelectionKey.OP_CONNECT);
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
        DataReceiver dataReceiver = dataReceivers.get(mainChannel);
        if (dataReceiver != null)
            dataReceiver.doReceive();
        else {
            dataReceiver = new DataReceiver(key, this);
            dataReceivers.put(mainChannel, dataReceiver);
            dataReceiver.doReceive();
        }
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
    void cancelConnection(SelectionKey key) throws IOException {
        super.cancelConnection(key);
        shouldExit = true;
        selector.wakeup();
    }

    @Override
    void connect(SelectionKey key) throws IOException {
        if (mainChannel.isConnectionPending()) {
            mainChannel.finishConnect();
            ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);
            mainChannel.register(selector, SelectionKey.OP_READ, buf);

            outgoingQueues.clear();
            outgoingQueues.put(mainChannel, new ConcurrentLinkedQueue<Message>());
            addMessageToOutgoing(new Message(Message.Type.PING, myName), mainChannel);
        }
    }

}
