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
public class BigDataReceiver extends Thread{

    private static final int MAX_SEND_TRIES = 1000;
    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private ByteArrayOutputStream collector = new ByteArrayOutputStream();
    private Message parentMessage = null;

    public BigDataReceiver(SelectionKey key, GeneralComm communicator, Message parentMessage) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        this.parentMessage = parentMessage;
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

            int size = buf.remaining();
            byte[] data = new byte[size];
            buf.get(data, 0, size);
            collector.write(data);

            if (collector.size() == (Integer) parentMessage.getData()) {
                NodeLogger.get().info("Received data for " + parentMessage);
                parentMessage.setData(collector.toByteArray());
                communicator.handleBigMessage(parentMessage, myChannel);
            } else {
                NodeLogger.get().debug("Message " + parentMessage.getId() + ": Received " + collector.size() + " bytes of " + parentMessage.getData());
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
