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
    private ByteArrayOutputStream collector = new ByteArrayOutputStream();
    private Message parentMessage = null;
//    private boolean isReceiving = false;

    public DataReceiver(SelectionKey key, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
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
                if (parentMessage == null) {
                    int size = buf.getInt();
                    NodeLogger.get().trace("Receiving message of size " + size);
                    byte[] data = new byte[size];
                    buf.get(data, 0, size);
                    Message message = Message.getFromBytes(data);
                    NodeLogger.get().debug("Received " + message);
                    if (message != null) {
                        if (Message.Type.get(message.getType()).isBulkType()) {
                            parentMessage = message;
                            continue;
                        } else {
                            communicator.handleMessage(message, myChannel);
                        }
                    }
                }
                if (parentMessage != null) {
                    int total = -1;
                    try {
                        total = (Integer) parentMessage.getData();
                    } catch (Exception e) {
                        NodeLogger.get().error("Cannot cast " + parentMessage.getData() + " for " + parentMessage);
                    }

                    int size = Math.min(buf.remaining(), total - collector.size());
                    byte[] data = new byte[size];
                    buf.get(data, 0, size);
                    collector.write(data);
                    if (collector.size() == total) {
                        NodeLogger.get().debug("Received data for " + parentMessage);
                        parentMessage.setData(collector.toByteArray());
                        communicator.handleMessage(parentMessage, myChannel);
                        parentMessage = null;
                        collector.close();
                        collector = new ByteArrayOutputStream();
                    } else {
                        NodeLogger.get().trace("Message " + parentMessage.getId() + ": Received " + collector.size() + " bytes of " + parentMessage.getData());
                    }
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
