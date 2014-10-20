package com.cclab.core;


import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;

import java.io.IOException;

public class MasterInstance extends NodeInstance {

    public MasterInstance(String myHostname, int port) throws IOException {
        super(myHostname);
        server = new ServerComm(port);
        server.start();
        try {
            Thread.sleep(10000);
            System.out.println("Sending final");
            Message message = new Message();
            message.setType(Message.Type.LOADINPUT.getCode());
            message.setOwner("ha");
            message.setDetails("dum dum dum");
            server.addMessageToQueue(message, "me");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
