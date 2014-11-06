package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * Created on 11/5/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class BigDataSender {

    private static final int MAX_SEND_TRIES = 1000;
    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private byte[] data = null;
    private int chunks = -1;
    private int chunk = 0;
    private Message parentMessage = null;

    public BigDataSender(SelectionKey key, byte[] data, Message parentMessage, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        this.parentMessage = parentMessage;
        this.data = data;
        chunks = data.length / BUF_SIZE;
        if (data.length % BUF_SIZE > 0)
            chunks++;
    }

    public void doSend() {
        NodeLogger.get().debug("Sending large data (" + data.length + " bytes)");
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);
        try {
            buf.clear();
            int size = Math.min(data.length - chunk * BUF_SIZE, BUF_SIZE);
            byte[] part = Arrays.copyOfRange(data, chunk * BUF_SIZE, chunk * BUF_SIZE + size);
            buf.put(part);
            buf.flip();
            int tries = 0;
            while (buf.hasRemaining()) {
                myChannel.write(buf);
                if (++tries > MAX_SEND_TRIES) {
                    NodeLogger.get().debug("Outgoing buffer full for " + ((SocketChannel) myKey.channel()).socket().getRemoteSocketAddress());
                    //TODO maybe disconnect
                }
            }
            NodeLogger.get().debug("Message " + parentMessage.getId() + ": Sent packet " + (chunk + 1) + " of " + chunks + "(" + size + " bytes)");
            chunk++;
            if(chunk == chunks){
                NodeLogger.get().info("Successfully finished sending "+parentMessage);
                communicator.finishedSending(parentMessage, myChannel);
            }
            myKey.interestOps(SelectionKey.OP_READ);
        } catch (Exception e) {
            try {
                if (buf != null)
                    buf.clear();
                communicator.cancelConnection(myKey);
            } catch (IOException ioe) {
                NodeLogger.get().error("Error cancelling connection ", ioe);
            }
        }
    }

}
