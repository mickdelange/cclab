package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Runnable combination of a receiver and a transmitter.
 * <p/>
 * When instantiated without a message to deliver, the transmitter tries to
 * read from its given key. Otherwise, it tries to write the message to the
 * key's channel. The calling communicator instance is required in order to
 * report communication errors and cancel the connection.
 * <p/>
 * Created on 10/29/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class Transceiver implements Runnable {
    private static final int MAX_SEND_TRIES = 1000;
    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private Message payload = null;

    public Transceiver(SelectionKey key, Message payload, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        this.payload = payload;
    }

    @Override
    public void run() {
        if (payload == null) {
            doReceive();
            myKey.selector().wakeup();
        } else {
            doSend();
        }
    }

    private synchronized void doSend() {
        byte[] data = payload.toBytes();
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length + 8);
        try {
            buf.clear();
            buf.putInt(payload.getId());
            buf.putInt(data.length);
            buf.put(data);
            buf.flip();
            int tries = 0;
            while (buf.hasRemaining()) {
                myChannel.write(buf);
                if (++tries > MAX_SEND_TRIES) {
                    NodeLogger.get().debug("Outgoing buffer full for " + ((SocketChannel) myKey.channel()).socket().getRemoteSocketAddress());
                    //TODO maybe disconnect
                }
            }
            NodeLogger.get().debug("Sent " + payload);
            myKey.interestOps(SelectionKey.OP_READ);

        } catch (Exception e) {
            try {
                buf.clear();
                communicator.cancelConnection(myKey);
            } catch (IOException ioe) {
                NodeLogger.get().error("Error cancelling connection ", ioe);
            }
        }
    }

    private void doReceive() {

        int bytes = -1;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE + 8);

        // read from socket into buffer, use a loop
        try {
            buf.clear();
            while ((bytes = myChannel.read(buf)) > 0)
                ;

            buf.flip();

            while (buf.limit() - buf.position() > 8) {
                System.out.println("");
                int id = buf.getInt();
                int size = buf.getInt();

                byte[] data = new byte[size];
                buf.get(data, 0, Math.min(buf.remaining(), size));
                Message message = Message.getFromBytes(data);
                NodeLogger.get().debug("Received " + message);
                communicator.handleMessage(message, myChannel);
            }

            if (myChannel.read(buf) == -1) {
                //node disconnected
                buf.clear();
                communicator.cancelConnection(myKey);
            }

        } catch (Exception e) {
            NodeLogger.get().error("Error receiving message ", e);
            //assume node disconnected
            buf.clear();
            try {
                communicator.cancelConnection(myKey);
            } catch (IOException ioe) {
                NodeLogger.get().error("Error cancelling connection ", ioe);
            }
        }
    }
}

