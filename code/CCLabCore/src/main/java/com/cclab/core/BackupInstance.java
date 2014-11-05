package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.utils.MasterObserver;

import java.io.IOException;

public class BackupInstance extends NodeInstance {
	
    int port;
    String masterIP = null;
    MasterObserver masterObserver;

    public BackupInstance(String myName, String masterIP, int port) throws IOException {
        super(myName);
        this.masterIP = masterIP;
        this.port = port;
        ClientComm client = new ClientComm(masterIP, port, myName, this);
        client.start();
        clients.put(masterIP, client);
        
        server = new ServerComm(port, myName, this);
        server.start();
        
        masterObserver = new MasterObserver(this);
        masterObserver.run();
    }
    
    /**
     * Take over from Master, notify all nodes.
     */
    public void takeOver() {
    	// TODO: implement
    }

	@Override
	public boolean extendedInterpret(String[] command) {
		return false;
	}

    @Override
    public void processMessage(Message message) {
    	// Notify observer of contact.
    	masterObserver.hadContact();
    	
        if (message.getType() == Message.Type.NEWTASK.getCode()) {
        	// TODO: process new task message:
        	// Store new image in input
        } else if (message.getType() == Message.Type.FINISHED.getCode()) {
        	// TODO: process task finished:
        	// Move image from input to output
        }
    }

    @Override
    public void nodeConnected(String name){
        super.nodeConnected(name);
    }
}
