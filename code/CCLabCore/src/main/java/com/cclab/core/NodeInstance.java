package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.network.MessageInterpreter;
import com.cclab.core.network.ServerComm;
import com.cclab.core.utils.CLInterpreter;
import com.cclab.core.utils.CLReader;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

/**
 * Created by ane on 10/19/14.
 */
public abstract class NodeInstance implements CLInterpreter, MessageInterpreter {

    ServerComm server = null;
    HashMap<String, ClientComm> clients = null;
    String myName;
    String masterIP = null;

    public NodeInstance(String myName) {
        this.myName = myName;
        NodeLogger.configureLogger(myName);
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
            if (command[0].equals("sendToWorker")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 3, " "));
                if (server != null)
                    server.addMessageToQueue(message, command[2]);
                return true;
            }
            if (command[0].equals("bcast")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 2, " "));
                if (server != null)
                    server.addMessageToQueue(message, (String) null);
                for (ClientComm client : clients.values())
                    client.addMessageToQueue(message);
                return true;
            }
            if (command[0].equals("sendToMaster")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 2, " "));
                ClientComm masterLink = clients.get(masterIP);
                if (masterLink != null)
                    masterLink.addMessageToQueue(message);
                else
                    NodeLogger.get().error("No link to master " + masterIP);
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + "(" + e.getMessage() + ")", e);
        }
        return extendedInterpret(command);
    }

    @Override
    public void checkIfNew(String clientName, SocketChannel socketChannel) {
        if (server != null)
            server.registerClient(clientName, socketChannel);
    }

    @Override
    public void disconnectClient(SocketChannel socketChannel) {
        if (server != null)
            server.removeSocketChannel(socketChannel);
    }

    //return false if should not continue reading CLI
    public abstract boolean extendedInterpret(String[] command);

}
