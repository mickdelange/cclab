package com.cclab.core.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ane on 10/20/14.
 */
public class GeneralSender {

    SelectionKey myKey;
    SocketChannel myChannel;

    public GeneralSender(SelectionKey key) throws IOException {
        this.myKey = key;
        this.myChannel = (SocketChannel) key.channel();
    }


}
