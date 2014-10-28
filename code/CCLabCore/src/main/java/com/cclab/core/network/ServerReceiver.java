package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ane on 10/19/14.
 */
public class ServerReceiver extends GeneralReceiver {

    public ServerReceiver(SelectionKey key, GeneralComm communicator) throws IOException {
        super(key, communicator);
    }

    @Override
    void cancelConnection() throws IOException{
        communicator.disconnectClient(myChannel);
    }

}
