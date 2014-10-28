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
 * Created by ane on 10/19/14.
 */
public class ServerComm extends GeneralComm {
    public ConcurrentHashMap<String, SocketChannel> clientToChannel;
    public ConcurrentHashMap<SocketChannel, String> channelToClient;
    public ExecutorService pool = Executors.newFixedThreadPool(5);
    private ConcurrentHashMap<SocketChannel, ConcurrentLinkedQueue<Message>> outgoingQueues;
    private ConcurrentLinkedQueue<SocketChannel> socketsToBeRemoved;
    ServerSocketChannel mainChannel;

    public ServerComm(int port, MessageInterpreter interpreter) throws IOException {
        super(port, interpreter);

        outgoingQueues = new ConcurrentHashMap<SocketChannel, ConcurrentLinkedQueue<Message>>();
        socketsToBeRemoved = new ConcurrentLinkedQueue<SocketChannel>();
        clientToChannel = new ConcurrentHashMap<String, SocketChannel>();
        channelToClient = new ConcurrentHashMap<SocketChannel, String>();
        messageParts = new ConcurrentHashMap<SocketChannel, ConcurrentHashMap<Integer, byte[]>>();

        initialize();
    }

    @Override
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

    void checkOutgoing() {
        for (SocketChannel sc : channelToClient.keySet()) {
            if (!outgoingQueues.get(sc).isEmpty()) {
                sc.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    @Override
    void accept(SelectionKey key) throws IOException {

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        socketChannel.register(key.selector(), SelectionKey.OP_READ);

        messageParts.put(socketChannel, new ConcurrentHashMap<Integer, byte[]>());

        // display remote client address
        NodeLogger.get().info(
                "Connection from: "
                        + socketChannel.socket().getRemoteSocketAddress()
        );
    }

    @Override
    void read(SelectionKey key) throws IOException {
        pool.execute(new Thread(new ServerReceiver(key, this)));
    }

    @Override
    void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        writeFromQueue(key, outgoingQueues.get(channel));
        if (socketsToBeRemoved.contains(channel)) {
            removeSocketChannel(channel);
        }
    }

    @Override
    void cleanup() {
        pool.shutdownNow();
        if (mainChannel != null)
            try {
                mainChannel.close();
            } catch (IOException e) {
            }
    }

    public void removeSocketChannel(SocketChannel socketChannel) {
        String client = channelToClient.get(socketChannel);

        NodeLogger.get().info("Node " + client + " disconnected");
        outgoingQueues.remove(socketChannel);
        channelToClient.remove(socketChannel);
        clientToChannel.remove(client);
        socketsToBeRemoved.remove(socketChannel);

        try {
            socketChannel.close();
        } catch (IOException e) {
            NodeLogger.get().error("Error closing client channel: " + e.getMessage(), e);
        }

    }

    public void registerClient(String client, SocketChannel channel) {
        if (channel.equals(clientToChannel.get(client)))
            return;
        NodeLogger.get().info("Client " + client + " has connected.");
        outgoingQueues.put(channel, new ConcurrentLinkedQueue<Message>());
        clientToChannel.put(client, channel);
        channelToClient.put(channel, client);
    }

    public void addMessageToQueue(Message message, SocketChannel clientChannel) {
        outgoingQueues.get(clientChannel).add(message);
        selector.wakeup();
    }

    public void addMessageToQueue(Message message, String client) {
        if (client == null) {
            //broadcast
            for (SocketChannel channel : channelToClient.keySet())
                addMessageToQueue(message, channel);
            return;
        }
        SocketChannel clientChannel = clientToChannel.get(client);
        if (clientChannel == null)
            NodeLogger.get().error("Client " + client + " not connected");
        addMessageToQueue(message, clientChannel);
    }

    @Override
    public void checkIfNew(String clientName, SocketChannel socketChannel) {
        registerClient(clientName, socketChannel);
    }

    @Override
    public void disconnectClient(SocketChannel socketChannel) {
        removeSocketChannel(socketChannel);
    }

    @Override
    void finishedReading(SelectionKey key) {
        /* deactivate interest for reading */
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
    }
}
