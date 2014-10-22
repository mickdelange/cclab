package com.cclab.core.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ane on 10/19/14.
 */
public class ServerReceiver extends GeneralReceiver {

    ServerComm serverCommInstance;

    public ServerReceiver(SelectionKey key, ServerComm serverCommInstance) throws IOException {
        super(key);
        this.serverCommInstance = serverCommInstance;
    }

    @Override
    void handleReceivedMessage(Message message) {
        //TODO interpret message
        serverCommInstance.registerClient(message.getOwner(), myChannel);
        System.out.println("Received " + message);
        serverCommInstance.addMessageToQueue(message, myChannel);
    }

    @Override
    void cancelConnection() throws IOException{
        serverCommInstance.removeSocketChannel(myChannel);
    }

}
