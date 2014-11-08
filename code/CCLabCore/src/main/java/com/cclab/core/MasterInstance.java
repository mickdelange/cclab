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
    String myBackupName;
    boolean backupConnected = false;

    public MasterInstance(String name, String backupName, int port) throws IOException {
        super(name);
        myBackupName = backupName;

        server = new ServerComm(port, myName, this);
        server.start();

        // Add all master nodes to a list. Currently only Master & Backup
        List<String> masterIds = new ArrayList<String>();
        masterIds.add(myName);
        masterIds.add(myBackupName);

        scheduler = new Scheduler(masterIds, this);
        scheduler.start();
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

    /**
     * Send a task to a specific node
     *
     * @param recipient Node
     * @param inputId   Task id
     */
    public void sendTaskTo(String recipient, String inputId) {
        if (inputId == null || inputId.length() < 1) {
            NodeLogger.get().error("Task input identifier not supplied");
            return;
        }
        NodeLogger.get().info("Sending task " + inputId + " to " + recipient);

        NodeLogger.getTasking().info("ASSIGN_" + inputId + "_" + recipient);
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

    /**
     * Back-up new task to backup node.
     *
     * @param inputId
     */
    public void backupNewTask(String inputId) {
        if (backupConnected) {
            Message message = new Message(Message.Type.BACKUPTASK, myName);
            message.setDetails(inputId);
            byte[] input = Database.getInstance().getRecord(inputId);
            if (input == null) {
                NodeLogger.get().error("Task will not be sent");
                return;
            }
            // TODO: fix: message.setData(input);
            server.addMessageToOutgoing(message, myBackupName);
        }
    }

    /**
     * Notify back-up of finished task.
     *
     * @param inputId
     */
    public void backupFinishedTask(String inputId) {
        if (backupConnected) {
            Message message = new Message(Message.Type.BACKUPFIN, myName);
            message.setDetails(inputId);
            server.addMessageToOutgoing(message, myBackupName);
        }
    }

    /**
     * Notify back-up of new connection.
     *
     * @param instanceId
     */
    public void backupNodeConnection(String instanceId) {
        if (backupConnected) {
            Message message = new Message(Message.Type.BACKUPCONNECT, myName);
            message.setDetails(instanceId);
            server.addMessageToOutgoing(message, myBackupName);
        }
    }

    /**
     * Notify back-up of still being alive.
     */
    public void backupStillAlive() {
        if (backupConnected) {
            Message message = new Message(Message.Type.STILLALIVE, myName);
            server.addMessageToOutgoing(message, myBackupName);
        }
    }

    @Override
    public void processMessage(Message message) {
        if (message.getType() == Message.Type.FINISHED.getCode()) {
            NodeLogger.get().info("Task " + message.getDetails() + " finished");

            scheduler.taskFinished(message.getOwner());

            // optional
            Database.getInstance().storeRecord((byte[]) message.getData(), message.getDetails());
            NodeLogger.getTasking().info("DONE_" + message.getDetails() + "_" + message.getOwner());
        }
    }

    @Override
    public void nodeConnected(String name) {
        super.nodeConnected(name);
        if (name.equals(myBackupName)) {
            // Handle connection to backup node
            backupConnected = true;
        } else {
            // Handle connection to worker node
            scheduler.nodeConnected(name);
            // Backup connection
            backupNodeConnection(name);
        }

    }

    @Override
    public void shutDown() {
        super.shutDown();
        scheduler.quit();
        NodeLogger.get().info("MASTER shutting down");
    }
}
