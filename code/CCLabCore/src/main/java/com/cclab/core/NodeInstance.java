package com.cclab.core;

import com.cclab.core.network.*;
import com.cclab.core.utils.CLInterpreter;
import com.cclab.core.utils.CLReader;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract implementation of a node.
 * <p/>
 * It keeps the reference to a server communicator that listens for connections
 * from other nodes and a series of client communicator references for
 * connecting to other nodes. The node listens for events from these
 * communicators. It is capable of interpreting command line instructions for
 * quiting and broadcasting messages.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public abstract class NodeInstance implements CLInterpreter, CommInterpreter {

    ServerComm server = null;
    HashMap<String, ClientComm> clients = null;
    public String myName;
    public String myIP;
    boolean shuttingDown = false;
    private CLReader clReader = null;

    public NodeInstance(String myName) {
        this.myName = myName;
		
        try {
			AwsConnect.init();
		} catch (Exception e) {
			NodeLogger.get().error(e.getMessage(), e);
		}
        
        myIP = AwsConnect.getInstancePrivIP(myName);
        NodeLogger.configureLogger(myName, this);
        clients = new HashMap<String, ClientComm>();
        clReader = new CLReader(this);
        clReader.start();
    }

    @Override
    public boolean interpretCommand(String[] command) {
        try {
            if (command[0].equals("quit")) {
            	shutDown();
                return false;
            }
            if (command[0].equals("bcast")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 2, " "));
                if (server != null)
                    server.addMessageToOutgoing(message, null);
                else
                    NodeLogger.get().error("Server down");
                for (ClientComm client : clients.values())
                    client.addMessageToOutgoing(message);
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return extendedInterpret(command);
    }


    //return false if should not continue reading CLI
    public abstract boolean extendedInterpret(String[] command);

    @Override
    public void communicatorDown(GeneralComm comm) {
        if (shuttingDown)
            return;
        if (comm.equals(server)) {
            NodeLogger.get().error("Server went down");
        } else {
            for (Map.Entry<String, ClientComm> e : clients.entrySet()) {
                if (comm.equals(e.getValue())) {
                    NodeLogger.get().error("Client for " + e.getKey() + " went down");
                }
            }
        }

    }

    @Override
    public void nodeConnected(String name) {
        NodeLogger.get().info("Node connected: "+name);
    }
    
    /**
     * Quit the instance
     */
    public void shutDown() {
    	shuttingDown = true;
        if(clReader != null)
            clReader.quit();
        if (server != null)
            server.quit();
        for (ClientComm client : clients.values())
            client.quit();
    }
}
