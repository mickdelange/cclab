package com.cclab.core.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ane on 10/19/14.
 */
public class ClientReceiver extends GeneralReceiver {

    public ClientReceiver(SelectionKey key) throws IOException {
        super(key);
    }

    @Override
    void handleReceivedMessage(Message message) {
        //TODO interpret message
        System.out.println("Received " + message);
    }

    @Override
    void cancelConnection() throws IOException {
        myKey.cancel();
        myKey.channel().close();
    }

}