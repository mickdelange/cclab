package com.cclab.core;

import com.cclab.core.network.*;
import com.cclab.core.utils.CLInterpreter;
import com.cclab.core.utils.CLReader;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ane on 10/19/14.
 */
public abstract class NodeInstance implements CLInterpreter, MessageInterpreter {

    ServerComm server = null;
    HashMap<String, ClientComm> clients = null;
    String myName;

    public NodeInstance(String myName) {
        this.myName = myName;
        NodeLogger.configureLogger(myName, this);
        clients = new HashMap<String, ClientComm>();
        new CLReader(this).start();
    }

    @Override
    public boolean interpretAndContinue(String[] command) {
        try {
            if (command[0].equals("quit")) {
                if (server != null)
                    server.quit();
                for (ClientComm client : clients.values())
                    client.quit();
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

}
