package com.cclab.core;


import com.cclab.core.network.ServerComm;

import java.io.IOException;

public class MasterInstance extends NodeInstance {

    public MasterInstance(String myHostname, int port) throws IOException {
        super(myHostname);
        server = new ServerComm(port);
        server.start();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        return true;
    }
}
