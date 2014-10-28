package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.processing.ImageProcessor;
import com.cclab.core.utils.NodeUtils;

import java.io.*;

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
	public void processMessage(Message message) throws IOException {
		// TODO Auto-generated method stub
        if(message.getType() == Message.Type.NEWTASK.getCode()){

            NodeUtils.writeDataToFile((byte[])message.getData(), "/Users/ane/Downloads/strawberry_done.jpg");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream((byte[])message.getData());
            ImageProcessor.process(in, out, "blur");
            Message ret = new Message(Message.Type.FINISHED, myName);
            ret.setDetails(message.getDetails());
            ret.setData(out.toByteArray());
            clients.get(masterIP).addMessageToQueue(ret);

//            NodeUtils.writeDataToFile((byte[])ret.getData(), "/Users/ane/Downloads/strawberry_bla.jpg");

        }
	}

}
