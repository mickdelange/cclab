package com.cclab.core;


import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;

import java.io.IOException;

public class MasterInstance extends NodeInstance {

    public MasterInstance(String myName, int port) throws IOException {
        super(myName);
        server = new ServerComm(port, this);
        server.start();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        return true;
    }

    @Override
    public void processMessage(Message message) throws IOException {
        //TODO
    }
}
