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
    private int chunk = 0;
    private Message payload = null;

    public DataSender(SelectionKey key, Message payload, GeneralComm communicator) {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
        this.payload = payload;
        this.data = payload.toBytes();
        chunks = data.length / BUF_SIZE;
        if (data.length % BUF_SIZE > 0)
            chunks++;
    }

    public boolean hasRemaining() {
        return chunk < chunks;
    }

    public void doSend() {
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE + 4);
        try {
            buf.clear();
            if (chunk == 0)
                buf.putInt(data.length);

            int size = Math.min(data.length - chunk * BUF_SIZE, BUF_SIZE);
            byte[] part = Arrays.copyOfRange(data, chunk * BUF_SIZE, chunk * BUF_SIZE + size);
            buf.put(part);
            buf.flip();
            int tries = 0;
            while (buf.hasRemaining()) {
                myChannel.write(buf);
                if (++tries > MAX_SEND_TRIES) {
                    NodeLogger.get().trace("Outgoing buffer full for " + ((SocketChannel) myKey.channel()).socket().getRemoteSocketAddress());
                    //TODO maybe disconnect
                }
            }
            NodeLogger.get().trace("Message " + payload.getId() + ": Sent " + (chunk + 1) + " of " + chunks + "(" + size + " bytes)");
            chunk++;
            if (chunk >= chunks) {
                NodeLogger.get().debug("Sent data for " + payload);
                communicator.finishedSending(payload, myChannel);
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
