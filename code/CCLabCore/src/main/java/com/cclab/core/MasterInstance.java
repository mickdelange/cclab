package com.cclab.core;


import java.io.InputStream;

public class MasterInstance extends NodeInstance {

    public MasterInstance() {
        super();
    }

    @Override
    public void processInput(NodeUtils.MessageType type, InputStream data) {
        switch (type) {
            case LOADOUTPUT: //TODO
                break;
            default:
                System.out.println("Type "+type+" not processed");
                break;
        }
    }

}
