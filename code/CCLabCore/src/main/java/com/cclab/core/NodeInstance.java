package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.ServerComm;

import java.util.HashMap;

/**
 * Created by ane on 10/19/14.
 */
public abstract class NodeInstance {

    ServerComm server;
    HashMap<String, ClientComm> clients;
    String myHostname;

    public NodeInstance(String myHostname) {
        this.myHostname = myHostname;
        NodeLogger.configureLogger(myHostname);
        clients = new HashMap<String, ClientComm>();
    }

    //public abstract void processMessage(Message message) throws IOException;
}
