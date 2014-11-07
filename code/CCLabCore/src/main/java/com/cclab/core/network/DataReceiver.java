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
public class DataReceiver {

    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private ByteArrayOutputStream collector = new ByteArrayOutputStream();
    private Message parentMessage = null;
    private boolean done = false;

    public DataReceiver(SelectionKey key, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
    }

    public boolean isDone() {
        return done;
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

            if (parentMessage == null) {
                while (buf.remaining() > 4) {
                    int size = buf.getInt();
                    NodeLogger.get().debug("Receiving message of size " + size);
                    byte[] data = new byte[size];
                    buf.get(data, 0, size);
                    Message message = Message.getFromBytes(data);
                    NodeLogger.get().debug("Received " + message);
                    if (message != null) {
                        if (message.getType() == Message.Type.NEWTASK.getCode() ||
                                message.getType() == Message.Type.FINISHED.getCode()) {
                            parentMessage = message;
                            break;
                        } else {
                            done = true;
                            communicator.handleMessage(message, myChannel);
                        }
                    }
                }
            }
            if (parentMessage != null) {
                int size = buf.remaining();
                byte[] data = new byte[size];
                buf.get(data, 0, size);
                collector.write(data);
                int total = -1;
                try {
                    total = (Integer) parentMessage.getData();
                } catch (Exception e) {
                    NodeLogger.get().error("Malformed parent " + parentMessage.getId() + " for " + parentMessage);
                }
                if (collector.size() == total) {
                    NodeLogger.get().info("Received data for " + parentMessage);
                    parentMessage.setData(collector.toByteArray());
                    done = true;
                    communicator.handleMessage(parentMessage, myChannel);
                } else {
                    NodeLogger.get().debug("Message " + parentMessage.getId() + ": Received " + collector.size() + " bytes of " + parentMessage.getData());
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
