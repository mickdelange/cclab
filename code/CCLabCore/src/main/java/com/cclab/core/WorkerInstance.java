package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;

import java.io.IOException;

/**
 * Created by ane on 10/15/14.
 */
public class WorkerInstance extends NodeInstance {

    public WorkerInstance(String myName, String masterIP, int port) throws IOException {
        super(masterIP);
        this.masterIP = masterIP;
        ClientComm client = new ClientComm(masterIP, port, myName, this);
        client.start();
        clients.put(masterIP, client);
    }

    @Override
    public boolean extendedInterpret(String[] command){
        return true;
    }

    @Override
    public void processMessage(Message message) throws IOException{
        //TODO
    }
}
