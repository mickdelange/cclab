package com.cclab.core.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by ane on 10/20/14.
 */
public abstract class GeneralReceiver implements Runnable {

    SelectionKey myKey;
    SocketChannel myChannel;
    static final int BUF_SIZE = 8192;


    public GeneralReceiver(SelectionKey key) throws IOException {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
    }

    @Override
    public void run() {
        try {
            reciveMessages();
            /* reactivate interest for read */
            myKey.selector().wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void reciveMessages() throws IOException {

        int bytes = -1;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE);//(ByteBuffer) key.attachment();

        // read from socket into buffer, use a loop
        try {
            buf.clear();
            while ((bytes = myChannel.read(buf)) > 0)
                ;

            buf.flip();

            while (buf.limit() - buf.position() > 4) {
                int size = buf.getInt();
                byte[] data = new byte[size];
                buf.get(data, 0, size);
                Message message = Message.getFromBytes(data);
                handleReceivedMessage(message);
            }

            if (myChannel.read(buf) == -1) {
                //node disconnected
                buf.clear();

            }

        } catch (IOException e) {
            //assume node disconnected
            buf.clear();
            cancelConnection();
        }
    }

    abstract void handleReceivedMessage(Message message);

    abstract void cancelConnection() throws IOException;
}
