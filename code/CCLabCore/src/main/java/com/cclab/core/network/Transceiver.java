package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * Created by ane on 10/29/14.
 */

public class Transceiver implements Runnable {
    private int MAX_SEND_TRIES = 1000;

    SelectionKey myKey = null;
    SocketChannel myChannel = null;
    static final int BUF_SIZE = 8192;
    GeneralComm communicator = null;
    Message payload = null;

    public Transceiver(SelectionKey key, Message payload, GeneralComm communicator) throws IOException {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        this.payload = payload;
    }

    @Override
    public void run() {
        try {
            if (payload == null) {
                doReceive();
                myKey.selector().wakeup();
            } else
                doSend();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void doSend() throws IOException {
        byte[] data = payload.toBytes();
        SocketChannel channel = (SocketChannel) myKey.channel();
        NodeLogger.get().info("Sending " + data.length + " bytes: " + payload);

        ByteBuffer buf = null;
        try {
            int chunks = data.length / BUF_SIZE;
            if (data.length % BUF_SIZE > 0)
                chunks++;
            int crt = 0;
            while (crt < chunks) {
                buf = ByteBuffer.allocateDirect(data.length + 16);
                int size = Math.min(data.length - crt*BUF_SIZE, BUF_SIZE);
                buf.putInt(payload.getId());
                buf.putInt(chunks);
                buf.putInt(crt);
                buf.putInt(size);
                byte[] part = Arrays.copyOfRange(data, crt*BUF_SIZE, crt*BUF_SIZE + size);
                buf.put(part);
                buf.flip();
                int tries = 0;
                while (buf.hasRemaining()) {
                    myChannel.write(buf);
                    if( ++tries > MAX_SEND_TRIES) {
                        NodeLogger.get().debug("Outgoing buffer full for " + ((SocketChannel) myKey.channel()).socket().getRemoteSocketAddress());
                        //TODO maybe disconnect
                    }
                }
                NodeLogger.get().debug("Sent packet " + (crt + 1) + " of " + chunks);
                crt++;
            }
            myKey.interestOps(SelectionKey.OP_READ);

        } catch (Exception e) {
            communicator.cancelConnection(myKey);
        }
    }

    private void doReceive() throws IOException {

        int bytes = -1;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE + 16);

        // read from socket into buffer, use a loop
        try {
            buf.clear();
            while ((bytes = myChannel.read(buf)) > 0)
                ;

            buf.flip();

            while (buf.limit() - buf.position() > 16) {
                int id = buf.getInt();
                int total = buf.getInt();
                int part = buf.getInt();
                int size = buf.getInt();
                NodeLogger.get().debug("Received packet " + (part + 1) + " of " + total + " (" + size + ")");

                byte[] data = new byte[size];
                buf.get(data, 0, size);
                communicator.handlePartialMessage(id, total, part, data, myChannel);
            }

            if (myChannel.read(buf) == -1) {
                //node disconnected
                buf.clear();
                communicator.cancelConnection(myKey);
            }

        } catch (Exception e) {
            NodeLogger.get().error("Error receiving message " + e);
            //assume node disconnected
            buf.clear();
            communicator.cancelConnection(myKey);
        }
    }
}

