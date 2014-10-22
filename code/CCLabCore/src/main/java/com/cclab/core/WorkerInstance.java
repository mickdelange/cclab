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
    }

    @Override
    public boolean extendedInterpret(String[] command){
        return true;
    }

	@Override
	public void processMessage(Message message) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
