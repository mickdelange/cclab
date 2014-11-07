package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.utils.BootObserver;
import com.cclab.core.utils.BootSettings;
import com.cclab.core.utils.MasterObserver;
import com.cclab.core.utils.NodeLogger;

import java.io.IOException;

public class BackupInstance extends NodeInstance {
	
    int port;
    String masterIP = null;
    String myMasterName;
    MasterObserver masterObserver;

    public BackupInstance(String myName, String masterName, String masterIP, int port) throws IOException {
        super(myName);
        myMasterName = masterName;
        this.masterIP = masterIP;
        this.port = port;
        ClientComm client = new ClientComm(masterIP, port, myName, this);
        client.start();
        clients.put(masterIP, client);
        
        masterObserver = new MasterObserver(this);
        
        try {
			AwsConnect.init();
		} catch (Exception e) {
			NodeLogger.get().error(e.getMessage(), e);
		}
    }
    
    /**
     * Take over from Master, notify all nodes.
     */
    public void takeOver() {
    	NodeLogger.get().info("Backup node is taking over from Master node.");
    	// Reboot Master node
    	// TODO: test first: AwsConnect.rebootInstance(myMasterName);
    	
    	try {
    		// Init self as Master, with Master as Backup
			new MasterInstance(myName, myMasterName, port);

	    	// Notify all Workers.
	    	notifyWorkers();
	    	
	    	new BootObserver(myMasterName, BootSettings.backup(myMasterName, myName, myIP));
	    	
	    	// Kill self
	    	shutDown();
		} catch (IOException e) {
			NodeLogger.get().error("Could not start master, backup failed");
            NodeLogger.get().error(e.getMessage(), e);
            System.exit(1);
		}
    }
    
    /**
     * Notify all registered Worker nodes of new master.
     */
    private void notifyWorkers() {
    	String ownIP = AwsConnect.getInstancePrivIP(myName);
    	Message notification = new Message(Message.Type.NEWMASTER, myName);
    	notification.setDetails(ownIP);
    	
    	for (String key: clients.keySet()) {
    		if (!key.equals(masterIP)) // Notify only worker nodes
    			clients.get(key).addMessageToOutgoing(notification);
    	}
    }
    
    /**
     * Register to a node.
     * @param nodeName
     */
    private void registerNode(String nodeName) {
    	String nodeIP = AwsConnect.getInstancePrivIP(nodeName);
    	try {
            ClientComm client = new ClientComm(nodeIP, port, myName, this);
            client.start();
        	clients.put(nodeIP, client);
		} catch (IOException e) {
            e.printStackTrace();
			NodeLogger.get().error("Could not register node.");
		}
    }

	@Override
	public boolean extendedInterpret(String[] command) {
		return false;
	}

    @Override
    public void processMessage(Message message) {
    	// Notify observer of contact.
    	masterObserver.hadContact();
		if (!masterObserver.started)// Run observer on first contact
			masterObserver.run();
    	
        if (message.getType() == Message.Type.BACKUPTASK.getCode()) {
        	// TODO: process new task message:
        	// Store new image in input
        } else if (message.getType() == Message.Type.BACKUPFIN.getCode()) {
        	// TODO: process task finished:
        	// Move image from input to output
        } else if (message.getType() == Message.Type.BACKUPCONNECT.getCode()) {
        	registerNode(message.getDetails());
        }else {
    		NodeLogger.get().error("BACKUP received unknown message.");
    	}
    }

    @Override
    public void nodeConnected(String name){
        super.nodeConnected(name);
    }
}
