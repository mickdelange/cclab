package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;

import java.io.IOException;

/**
 * Created by ane on 10/15/14.
 */
public class WorkerInstance extends NodeInstance {

    public WorkerInstance(String myHostname, String masterHostname, int port) throws IOException {
        super(myHostname);
        ClientComm client = new ClientComm(masterHostname, port);
        client.start();
        clients.put(masterHostname + ":" + port, client);
        try {
            Message message;
            message = new Message();
            message.setType(Message.Type.LOADOUTPUT.getCode());
            message.setOwner("me");
            message.setDetails("bla bla bla");
            client.addMessageToQueue(message);
            Thread.sleep(1000);
            message = new Message();
            message.setType(Message.Type.LOADOUTPUT.getCode());
            message.setOwner("me");
            message.setDetails("and again");
            client.addMessageToQueue(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
