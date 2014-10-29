package com.cclab.core;

import com.cclab.core.network.*;
import com.cclab.core.utils.CLInterpreter;
import com.cclab.core.utils.CLReader;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
            if (command[0].equals("sendToWorker")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 3, " "));
                if (server != null)
                    server.addMessageToOutgoing(message, command[2]);
                else
                    NodeLogger.get().error("Server down");
                return true;
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
            if (command[0].equals("sendToMaster")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 2, " "));
                ClientComm masterLink = clients.get(masterIP);
                if (masterLink != null)
                    masterLink.addMessageToOutgoing(message);
                else
                    NodeLogger.get().error("No link to master " + masterIP);
                return true;
            }
            if (command[0].equals("sendTask")) {
                Message message = new Message(Message.Type.NEWTASK, myName);
                String filename = NodeUtils.join(command, 2, " ");

//                if(filename == null || filename.length()<1)
                filename = "/Users/ane/Downloads/strawberry.jpg";
                message.setDetails(filename);
                File infile = new File(filename);
                byte[] bytes = readBytesFromFile(infile);
                System.out.println("File length " + bytes.length);
                message.setData(bytes);
                if (server != null)
                    server.addMessageToOutgoing(message, command[1]);
                else
                    NodeLogger.get().error("Server down");
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return extendedInterpret(command);
    }

    private static byte[] readBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            throw new IOException("Could not completely read file " + file.getName() + " as it is too long (" + length + " bytes, max supported " + Integer.MAX_VALUE + ")");
        }

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
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
