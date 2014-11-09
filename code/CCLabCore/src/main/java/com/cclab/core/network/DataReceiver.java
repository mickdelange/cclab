package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created on 11/5/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class DataReceiver extends Thread {

    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private ByteArrayOutputStream collector = null;
    private int expected = 0;

    public DataReceiver(SelectionKey key, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        collector = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
        doReceive();
    }

    public void doReceive() {
        int bytes = -1;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);

        // read from socket into buffer, use a loop
        try {
            buf.clear();
            while ((bytes = myChannel.read(buf)) > 0)
                ;

            buf.flip();

            while (buf.remaining() > 0) {
                if (collector.size() <= 0) {
                    expected = buf.getInt();
                    NodeLogger.get().debug("Expecting " + expected + " bytes");
                }

                int size = Math.min(buf.remaining(), expected - collector.size());
                byte[] data = new byte[size];
                buf.get(data, 0, size);
                collector.write(data);
                if (collector.size() == expected) {
                    Message message = Message.getFromBytes(collector.toByteArray());
                    if (message != null) {
                        NodeLogger.get().debug("Received data for " + message);
                        communicator.handleMessage(message, myChannel);
                        collector.close();
                        collector = new ByteArrayOutputStream();
                        expected = 0;
                    }
                } else {
                    NodeLogger.get().trace("Received " + collector.size() + " bytes of " + expected);
                }
            }

            if (myChannel.read(buf) == -1) {
                //node disconnected
                buf.clear();
                communicator.cancelConnection(myKey);
            }

        } catch (IOException e) {
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
