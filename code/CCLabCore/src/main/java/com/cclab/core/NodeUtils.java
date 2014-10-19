package com.cclab.core;

import org.apache.log4j.*;
import org.apache.log4j.varia.LevelRangeFilter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ane on 10/15/14.
 */
public class NodeUtils {
    static final long FSIZE = 183678375L;
    static final int PORT = 9026, SENDSIZE = 4094;

    static enum MessageType {
        LOADINPUT((byte)1),
        LOADOUTPUT((byte)2);

        private static final Map<Byte, MessageType> lookup = new HashMap<Byte, MessageType>();

        static {
            for (MessageType s : EnumSet.allOf(MessageType.class))
                lookup.put(s.getCode(), s);
        }

        private byte code;

        private MessageType(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static MessageType get(byte code) {
            return lookup.get(code);
        }
    }

    public static void configureLogger(String hostname, String role) {
        Logger logger = Logger.getLogger("NodeLogger." + hostname+"."+role);
        logger.setLevel(Level.DEBUG);
        ConsoleAppender consoleApp = new ConsoleAppender(new PatternLayout(
                "%-4r [%t] %-5p %c - %m%n"));
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.INFO);
        consoleApp.addFilter(filter);
        logger.addAppender(consoleApp);
        try {
            RollingFileAppender fileApp = new RollingFileAppender(
                    new PatternLayout("%d [%t] %-5p - %m%n"), "server_log_"
                    + hostname + ".txt");
            fileApp.setMaxFileSize("100KB");
            logger.addAppender(fileApp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class Server extends Thread {

        ServerSocketChannel listener = null;

        public Server() {
            this(PORT);
        }

        public Server(int port) {
            InetSocketAddress listenAddr = new InetSocketAddress(port);

            try {
                listener = ServerSocketChannel.open();
                ServerSocket ss = listener.socket();
                ss.setReuseAddress(true);
                ss.bind(listenAddr);
                System.out.println("Listening on port : " + listenAddr.toString());
            } catch (IOException e) {
                System.out.println("Failed to bind, is port : " + listenAddr.toString()
                        + " already in use ? Error Msg : " + e.getMessage());
                e.printStackTrace();
            }


        }

        @Override
        public void run() {
            ByteBuffer dst = ByteBuffer.allocate(SENDSIZE);
            try {
                while (true) {
                    SocketChannel conn = listener.accept();
                    System.out.println("Accepted : " + conn);
                    conn.configureBlocking(true);
                    int nread = 0;
                    MessageType type = null;
                    while (nread != -1) {
                        try {
                            nread = conn.read(dst);
                            System.out.println("Received " + nread);
                            if (type == null){
                                type = MessageType.get(dst.get(1));
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            nread = -1;
                        }
                        dst.rewind();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static SocketChannel getChannel(String host) throws IOException{
        SocketAddress sad = new InetSocketAddress(host, PORT);
        SocketChannel sc = SocketChannel.open();
        sc.connect(sad);
        sc.configureBlocking(true);
        return sc;
    }

    public static void sendfile(String host, MessageType messageType, String filePath) throws IOException {
        SocketChannel sc = getChannel(host);

        FileChannel fc = new FileInputStream(filePath).getChannel();
        long start = System.currentTimeMillis();
        long curnset = fc.transferTo(0, FSIZE, sc);
        System.out.println("total bytes transferred--" + curnset + " and time taken in MS--" + (System.currentTimeMillis() - start));
        //fc.close();
    }

    public static void sendData(String host, MessageType messageType, OutputStream out) throws IOException{
        getChannel(host);
        DataOutputStream s = new DataOutputStream(out);

    }

}
