package com.cclab.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class NodeUtils {
    static final long fsize = 183678375L, sendzise = 4094;
    static final int port = 9026;

   static class Server  {
        ServerSocketChannel listener = null;
        protected void mySetup()
        {
            InetSocketAddress listenAddr =  new InetSocketAddress(port);

            try {
                listener = ServerSocketChannel.open();
                ServerSocket ss = listener.socket();
                ss.setReuseAddress(true);
                ss.bind(listenAddr);
                System.out.println("Listening on port : "+ listenAddr.toString());
            } catch (IOException e) {
                System.out.println("Failed to bind, is port : "+ listenAddr.toString()
                        + " already in use ? Error Msg : "+e.getMessage());
                e.printStackTrace();
            }

        }

        private void readData()  {
            ByteBuffer dst = ByteBuffer.allocate(4096);
            try {
                while(true) {
                    SocketChannel conn = listener.accept();
                    System.out.println("Accepted : "+conn);
                    conn.configureBlocking(true);
                    int nread = 0;
                    while (nread != -1)  {
                        try {
                            nread = conn.read(dst);
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

    public static void sendfile(String host, String filePath) throws IOException {
        SocketAddress sad = new InetSocketAddress(host, port);
        SocketChannel sc = SocketChannel.open();
        sc.connect(sad);
        sc.configureBlocking(true);

        FileChannel fc = new FileInputStream(filePath).getChannel();
        long start = System.currentTimeMillis();
        long curnset =  fc.transferTo(0, fsize, sc);
        System.out.println("total bytes transferred--"+curnset+" and time taken in MS--"+(System.currentTimeMillis() - start));
        //fc.close();
    }

}
