package com.cclab.core.utils;

import com.cclab.core.network.Message;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by ane on 10/15/14.
 */
@Deprecated
public class GeneralFileComm {
    private static final long FSIZE = 183678375L;
    private static final int SENDSIZE = 4094;
    public static final int BUF_SIZE = 8192;

    public static final int DEFAULT_PORT = 9026;

    static class Server extends Thread {

        ServerSocketChannel listener = null;

        public Server() {
            this(DEFAULT_PORT);
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
                    Message.Type type = null;
                    while (nread != -1) {
                        try {
                            nread = conn.read(dst);
                            System.out.println("Received " + nread);
                            if (type == null) {
                                type = Message.Type.get(dst.get(1));
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

    private static SocketChannel getChannel(String host) throws IOException {
        SocketAddress sad = new InetSocketAddress(host, DEFAULT_PORT);
        SocketChannel sc = SocketChannel.open();
        sc.connect(sad);
        sc.configureBlocking(true);
        return sc;
    }

    public static void sendfile(String host, Message.Type messageType, String filePath) throws IOException {
        SocketChannel sc = getChannel(host);

        FileChannel fc = new FileInputStream(filePath).getChannel();
        long start = System.currentTimeMillis();
        long curnset = fc.transferTo(0, FSIZE, sc);
        System.out.println("total bytes transferred--" + curnset + " and time taken in MS--" + (System.currentTimeMillis() - start));
        //fc.close();
    }

    public static void sendData(String host, Message.Type messageType, OutputStream out) throws IOException {
        getChannel(host);
        DataOutputStream s = new DataOutputStream(out);

    }

}
