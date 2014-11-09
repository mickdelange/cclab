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
public class DataSender {

    private static final int MAX_SEND_TRIES = 1000;
    private SelectionKey myKey = null;
    private SocketChannel myChannel = null;
    private static final int BUF_SIZE = 8192;
    private GeneralComm communicator = null;
    private byte[] data = null;
    private int chunks = 0;
    private int chunk = -1;
    private Message parentMessage = null;

    public DataSender(SelectionKey key, Message parentMessage, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        if (Message.Type.get(parentMessage.getType()).isBulkType()) {
            this.data = (byte[]) parentMessage.getData();
            parentMessage.setData(data.length);
            chunks = data.length / BUF_SIZE;
            if (data.length % BUF_SIZE > 0)
                chunks++;
        }
        this.parentMessage = parentMessage;
    }

    public boolean hasRemaining() {
        return chunk < chunks;
    }

    public void doSend() {
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);
        try {
            buf.clear();
            int size;
            if (chunk < 0) {
                byte[] part = parentMessage.toBytes();
                size = part.length;
                buf.putInt(size);
                buf.put(part);
            } else {
                size = Math.min(data.length - chunk * BUF_SIZE, BUF_SIZE);
                byte[] part = Arrays.copyOfRange(data, chunk * BUF_SIZE, chunk * BUF_SIZE + size);
                buf.put(part);
            }
            buf.flip();
            int tries = 0;
            while (buf.hasRemaining()) {
                myChannel.write(buf);
                if (++tries > MAX_SEND_TRIES) {
                    NodeLogger.get().trace("Outgoing buffer full for " + ((SocketChannel) myKey.channel()).socket().getRemoteSocketAddress());
                    //TODO maybe disconnect
                }
            }
            NodeLogger.get().trace("Message " + parentMessage.getId() + ": Sent " + (chunk + 1) + " of " + chunks + "(" + size + " bytes)");
            chunk++;
            if (chunk >= chunks) {
                NodeLogger.get().debug("Sent data for " + parentMessage);
                communicator.finishedSending(parentMessage, myChannel);
            }
            myKey.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
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
