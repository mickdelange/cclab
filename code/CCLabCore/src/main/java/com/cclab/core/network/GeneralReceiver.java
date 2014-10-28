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
    GeneralComm communicator;
    //MessageInterpreter interpreter;


    public GeneralReceiver(SelectionKey key, GeneralComm communicator) throws IOException {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
        this.communicator = communicator;
    }

    @Override
    public void run() {
        try {
            receiveMessages();
            /* reactivate interest for read */
            myKey.selector().wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void receiveMessages() throws IOException {

        int bytes = -1;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUF_SIZE + 12);//(ByteBuffer) key.attachment();

        // read from socket into buffer, use a loop
        try {
            buf.clear();
            while ((bytes = myChannel.read(buf)) > 0)
                ;

            buf.flip();

            while (buf.limit() - buf.position() > 12) {
                int total = buf.getInt();
                int part = buf.getInt();
                int size = buf.getInt();
                byte[] data = new byte[size];
                buf.get(data, 0, size);
                System.out.println("Read " + size + " of part " + part + " of " + total);
                communicator.handlePartialMessage(total, part, data, myChannel);
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

    abstract void cancelConnection() throws IOException;
}
