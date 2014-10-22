package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by ane on 10/19/14.
 */
public class ServerReceiver extends GeneralReceiver {

    public ServerReceiver(SelectionKey key, MessageInterpreter interpreter) throws IOException {
        super(key, interpreter);
    }

    @Override
    void cancelConnection() throws IOException{
        interpreter.disconnectClient(myChannel);
    }

}
