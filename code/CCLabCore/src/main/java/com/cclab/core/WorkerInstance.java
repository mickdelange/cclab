package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.GeneralComm;
import com.cclab.core.network.Message;
import com.cclab.core.processing.ImageProcessor;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by ane on 10/15/14.
 */
public class WorkerInstance extends NodeInstance {
    int port;

    public WorkerInstance(String myName, String masterIP, int port) throws IOException {
        super(myName);
        this.masterIP = masterIP;
        this.port = port;
        ClientComm client = new ClientComm(masterIP, port, myName, this);
        client.start();
        clients.put(masterIP, client);
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        return true;
    }

    @Override
    public void processMessage(Message message) {
        // TODO Auto-generated method stub
        try {
            if (message.getType() == Message.Type.NEWTASK.getCode()) {

                NodeUtils.writeDataToFile((byte[]) message.getData(), "/Users/ane/Downloads/strawberry_done.jpg");

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayInputStream in = new ByteArrayInputStream((byte[]) message.getData());
                ImageProcessor.process(in, out, "blur");
                Message ret = new Message(Message.Type.FINISHED, myName);
                ret.setDetails(message.getDetails());
                ret.setData(out.toByteArray());
                clients.get(masterIP).addMessageToOutgoing(ret);

//            NodeUtils.writeDataToFile((byte[])ret.getData(), "/Users/ane/Downloads/strawberry_bla.jpg");

            }
        } catch (Exception e) {

        }
    }

    @Override
    public void communicatorDown(GeneralComm comm) {
//        super.communicatorDown(comm);
        try {

            if (comm.equals(clients.get(masterIP))) {
                ClientComm client = new ClientComm(masterIP, port, myName, this);
                client.start();
                clients.put(masterIP, client);
            }
        } catch (Exception e) {
            NodeLogger.get().error("Failed to restart client", e);
        }

    }
}
