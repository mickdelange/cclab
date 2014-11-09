package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extension of the standard communicator to act as a network server.
 * <p/>
 * Constructor has the same parameters of a GeneralComm. In addition, this
 * communicator keeps record of the names of the clients it communicates with
 * and offers an 'addMessageToOutgoing' method that can interpret the name of
 * the recipient. Outgoing messages are handled in a serial fashion. Incoming
 * messages are processed in parallel by a pool of executor threads.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class ServerComm extends GeneralComm {

    ServerSocketChannel mainChannel;
    ConcurrentHashMap<String, SocketChannel> nameToChannel;
    ConcurrentHashMap<SocketChannel, String> channelToName;
    ExecutorService pool = Executors.newFixedThreadPool(5);
    ConcurrentHashMap<SocketChannel, Transceiver> transceivers;
    public boolean listeningModeOn = false;

    public ServerComm(int port, String myName, CommInterpreter interpreter) throws IOException {
        super(port, myName, interpreter);

        nameToChannel = new ConcurrentHashMap<String, SocketChannel>();
        channelToName = new ConcurrentHashMap<SocketChannel, String>();
        transceivers = new ConcurrentHashMap<SocketChannel, Transceiver>();

        initialize();
    }

    void initialize() throws IOException {
        NodeLogger.get().info("Server communicator is now online");
        NodeLogger.get().info("Listening on port " + port);

        // init server socket and register it with a selector
        mainChannel = ServerSocketChannel.open();
        mainChannel.configureBlocking(false);
        mainChannel.socket().bind(new InetSocketAddress(port));

        selector = Selector.open();
        mainChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void registerClient(String client, SocketChannel channel) {
        if (channel.equals(nameToChannel.get(client)))
            return;
        NodeLogger.get().info("Client " + client + " has connected.");
        outgoingQueues.put(channel, new ConcurrentLinkedQueue<Message>());
        nameToChannel.put(client, channel);
        channelToName.put(channel, client);
        interpreter.nodeConnected(client);
    }

    public void addMessageToOutgoing(Message message, String client) {
        if (client == null) {
            //broadcast
            for (SocketChannel channel : channelToName.keySet())
                addMessageToOutgoing(message, channel);
            return;
        }
        SocketChannel clientChannel = nameToChannel.get(client);
        if (clientChannel == null) {
            NodeLogger.get().error("Client " + client + " not connected");
            return;
        }
        addMessageToOutgoing(message, clientChannel);
    }

    @Override
    void accept(SelectionKey key) throws IOException {

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        System.out.println(socketChannel);

        socketChannel.register(key.selector(), SelectionKey.OP_READ);

        NodeLogger.get().info("Connection from: " + socketChannel.socket().getRemoteSocketAddress()
        );
    }

    @Override
    void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        DataReceiver dataReceiver = dataReceivers.get(channel);
        if (dataReceiver != null) {
            dataReceiver.doReceive();
//            pool.execute(dataReceiver);
        } else {
            dataReceiver = new DataReceiver(key, this);
            dataReceivers.put(channel, dataReceiver);
            dataReceiver.doReceive();
//            pool.execute(dataReceiver);
        }
    }

    @Override
    void cleanup() {
        pool.shutdownNow();
        try {
            mainChannel.close();
            for (SocketChannel channel : channelToName.keySet()) {
                channel.keyFor(selector).cancel();
                channel.close();
            }
        } catch (IOException e) {
            NodeLogger.get().warn("Error cleaning up communicator ", e);
        }
        super.cleanup();
    }

    @Override
    void cancelConnection(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        String client = channelToName.get(channel);

        NodeLogger.get().info("Node " + client + " disconnected");
        interpreter.nodeDisconnected(client);
        channelToName.remove(channel);
        nameToChannel.remove(client);
        super.cancelConnection(key);
    }

    @Override
    void handleMessage(Message message, SocketChannel channel) {
        SelectionKey key = channel.keyFor(selector);
        registerClient(message.getOwner(), channel);
        if (!listeningModeOn)
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        super.handleMessage(message, channel);
    }

    @Override
    void connect(SelectionKey key) throws IOException {
        NodeLogger.get().error("Cannot handle connect");
    }

    public boolean hasOutgoingWaiting(String name) {
        SocketChannel channel = nameToChannel.get(name);
        if (channel == null)
            return false;
        ConcurrentLinkedQueue<Message> queue = outgoingQueues.get(channel);
        return !queue.isEmpty();
    }
}
