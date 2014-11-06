package com.cclab.core;


import com.cclab.core.data.Database;
import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.scheduler.Scheduler;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of NodeInstance that acts as a master node.
 * <p/>
 * This node keeps an internal scheduler for coordinating task execution. The
 * constructor also requires the port on which to listen for connections from
 * worker nodes. It is capable of interpreting command line instructions for
 * sending messages and tasks to workers, and storing the task results.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class MasterInstance extends NodeInstance {

    Scheduler scheduler;

    public MasterInstance(String myName, int port) throws IOException {
        super(myName);
        server = new ServerComm(port, myName, this);
        server.start();

        // Allow for adding multiple masterIds, currently only populated with own Id.
        List<String> masterIds = new ArrayList<String>();
        masterIds.add(myName);

        scheduler = new Scheduler(masterIds, this);
        scheduler.run();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        try {
            if (command[0].equals("sendTo")) {
                Message message = new Message(Message.Type.get(command[2]), myName);
                message.setDetails(NodeUtils.join(command, 3, " "));
                server.addMessageToOutgoing(message, command[1]);
                return true;
            }
            if (command[0].equals("sendTaskTo")) {
                String inputId = NodeUtils.join(command, 2, " ");
                String recipient = command[1];
                sendTaskTo(recipient, inputId);
                return true;
            }
            if (command[0].equals("sendNextTaskTo")) {
                String inputId = Database.getInstance().getNextRecordId();
                String recipient = command[1];
                sendTaskTo(recipient, inputId);
                return true;
            }
            if (command[0].equals("peekNextTask")) {
                String inputId = Database.getInstance().peekNextRecordId();
                System.out.println("Next task: " + inputId);
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return true;
    }

    public void sendTaskTo(String recipient, String inputId) {
        if (inputId == null || inputId.length() < 1) {
            NodeLogger.get().error("Task input identifier not supplied");
            return;
        }
        Message message = new Message(Message.Type.NEWTASK, myName);
        message.setDetails(inputId);
        byte[] input = Database.getInstance().getRecord(inputId);
        if (input == null) {
            NodeLogger.get().error("Task will not be sent");
            return;
        }
        message.setData(input);
        server.addMessageToOutgoing(message, recipient);
    }

    @Override
    public void processMessage(Message message) {
        if (message.getType() == Message.Type.FINISHED.getCode()) {
            NodeLogger.get().info("Task " + message.getDetails() + " finished");

            scheduler.taskFinished(message.getOwner());

            // optional
            Database.getInstance().storeRecord((byte[]) message.getData(), message.getDetails());
        }
    }

    @Override
    public void nodeConnected(String name) {
        super.nodeConnected(name);
        scheduler.nodeConnected(name);
    }
}
