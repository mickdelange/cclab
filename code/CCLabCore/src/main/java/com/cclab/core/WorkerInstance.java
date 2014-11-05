package com.cclab.core;

import com.cclab.core.network.ClientComm;
import com.cclab.core.network.GeneralComm;
import com.cclab.core.network.Message;
import com.cclab.core.processing.ProcessController;
import com.cclab.core.processing.Processor;
import com.cclab.core.processing.image.ImageProcessor;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;

/**
 * Extension of NodeInstance that acts as a worker node.
 * <p/>
 * The constructor also requires the master node's IP address and the port to
 * which it connects the main client. It is capable of interpreting command line
 * instructions for reporting back to the master.
 * <p/>
 * Created on 10/15/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class WorkerInstance extends NodeInstance implements ProcessController {
    int port;
    String masterIP = null;

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
        try {
            if (command[0].equals("report")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 2, " "));
                ClientComm masterLink = clients.get(masterIP);
                if (masterLink != null)
                    masterLink.addMessageToOutgoing(message);
                else
                    NodeLogger.get().error("No link to master " + masterIP);
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return true;
    }

    @Override
    public void processMessage(Message message) {
        if (message.getType() == Message.Type.NEWTASK.getCode()) {
//            Processor processor = new ImageProcessor(message.getDetails(), (byte[])message.getData(), "blur", this);
//            new Thread(processor).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message ret = new Message(Message.Type.FINISHED, myName);
            ret.setDetails(message.getDetails());
            clients.get(masterIP).addMessageToOutgoing(ret);
        }
    }

    @Override
    public void communicatorDown(GeneralComm comm) {
        super.communicatorDown(comm);
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

    @Override
    public void handleProcessorOutput(String taskId, byte[] output) {
        Message ret = new Message(Message.Type.FINISHED, myName);
        ret.setDetails(taskId);
        ret.setData(output);
        clients.get(masterIP).addMessageToOutgoing(ret);
    }
}
