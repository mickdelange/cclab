package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.utils.CLInterpreter;
import com.cclab.core.utils.CLReader;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by ane on 10/19/14.
 */
public abstract class NodeInstance implements CLInterpreter {

    ServerComm server;
    HashMap<String, ClientComm> clients;
    String myHostname;

    public NodeInstance(String myHostname) {
        this.myHostname = myHostname;
        NodeLogger.configureLogger(myHostname);
        clients = new HashMap<String, ClientComm>();
        new CLReader(this).start();
    }

    @Override
    public boolean interpretAndContinue(String[] command) {
        try {
            if (command[0].equals("quit")) {
                server.quit();
                for (ClientComm client : clients.values())
                    client.quit();
                return false;
            }
            if (command[0].equals("sendTo")) {
                Message message = new Message(Message.Type.get(command[1]), command[2]);
                message.setDetails(NodeUtils.join(command, 3, " "));
                return true;
            }
            if (command[0].equals("bcast")) {
                Message message = new Message(Message.Type.get(command[1]), null);
                message.setDetails(NodeUtils.join(command, 2, " "));
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Bad CLI command " + NodeUtils.join(command, " "));
        }
        return extendedInterpret(command);
    }

    //return false if should not continue reading CLI
    public abstract boolean extendedInterpret(String[] command);


    public abstract void processMessage(Message message) throws IOException;
}
