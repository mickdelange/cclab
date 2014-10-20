package com.cclab.core.network;

import com.cclab.core.NodeLogger;

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

    public ServerComm(int port) throws IOException {
        super(port);
        outgoingQueues = new ConcurrentHashMap<SocketChannel, ConcurrentLinkedQueue<Message>>();
        socketsToBeRemoved = new ConcurrentLinkedQueue<SocketChannel>();
        clientToChannel = new ConcurrentHashMap<String, SocketChannel>();
        channelToClient = new ConcurrentHashMap<SocketChannel, String>();

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

        // display remote client address
        NodeLogger.get().info(
                "Connection from: "
                        + socketChannel.socket().getRemoteSocketAddress()
        );
    }

    @Override
    void read(SelectionKey key) throws IOException {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
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

    protected void removeSocketChannel(SocketChannel socketChannel) throws IOException {
        String client = channelToClient.get(socketChannel);

        NodeLogger.get().info("Node " + client + " disconnected");
        outgoingQueues.remove(socketChannel);
        channelToClient.remove(socketChannel);
        clientToChannel.remove(client);
        socketsToBeRemoved.remove(socketChannel);

        socketChannel.close();

    }

    protected void registerClient(String client, SocketChannel channel) {
        if (channel.equals(clientToChannel.get(client)))
            return;
        outgoingQueues.put(channel, new ConcurrentLinkedQueue<Message>());
        clientToChannel.put(client, channel);
        channelToClient.put(channel, client);
    }

    public void addMessageToQueue(Message message, SocketChannel clientChannel) {
        outgoingQueues.get(clientChannel).add(message);
        selector.wakeup();
    }

    public void addMessageToQueue(Message message, String client) {
        SocketChannel clientChannel = clientToChannel.get(client);
        System.out.println("Found " + clientChannel);
        addMessageToQueue(message, clientChannel);
        System.out.println("annoying " + outgoingQueues.get(clientChannel).size());
    }
}
