package com.cclab.core;


import com.cclab.core.data.Database;
import com.cclab.core.network.GeneralComm;
import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.redundancy.DataReplicator;
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
    DataReplicator replicator = null;
    List<String> processing = null;
    boolean tasksStarted = false;

    public MasterInstance(String name, String backupName, int port) throws IOException {
        super(name);

        NodeLogger.getFailure().info("MASTER_BOOT");

        myBackupName = backupName;

        replicator = new DataReplicator(this);

        server = new ServerComm(port, myName, this);
        server.start();

        // Add all master nodes to a list. Currently only Master & Backup
        List<String> masterIds = new ArrayList<String>();
        masterIds.add(myName);
        masterIds.add(myBackupName);

        processing = new ArrayList<String>();

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
            if (command[0].equals("boot")) {
                scheduler.startNode(command[1]);
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
        if (!tasksStarted) {
            NodeLogger.getFailure().info("MASTER_PROCESSING");
            tasksStarted = true;
        }
        if (inputId == null || inputId.length() < 1) {
            NodeLogger.get().error("Task input identifier not supplied");
            return;
        }

        if (processing.contains(inputId)) {
            NodeLogger.get().info("Task is already being processed");
            scheduler.taskFinished(recipient, inputId);
            return;
        }

        NodeLogger.get().info("Sending task " + inputId + " to " + recipient);

        byte[] input = Database.getInstance().getRecord(inputId);
        if (input == null || input.length <= 0) {
            NodeLogger.get().error("Task will not be sent");
            return;
        }
        if (backupConnected)
            replicator.backupPendingRecord(inputId, input);
        else
            replicator.backupFutureRecord(inputId);
        NodeLogger.getTasking().info("ASSIGN_" + inputId + "_" + recipient);
        Message message = new Message(Message.Type.NEWTASK, myName);
        message.setDetails(inputId);
        message.setData(input);
        server.addMessageToOutgoing(message, recipient);
    }

    /**
     * Send a message to the backup master
     *
     * @param message
     */
    public void sendToBackup(Message message) {
        if (backupConnected) {
            server.addMessageToOutgoing(message, myBackupName);
        } else {
            NodeLogger.get().error("Backup is not connected! Will not send " + message);
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
     * Backup future task
     *
     * @param recordId
     */
    public void backupFutureTask(String recordId) {
        replicator.backupFutureRecord(recordId);
    }

    /**
     * Notify back-up of still being alive.
     */
    public void backupStillAlive() {
        if (backupConnected) {
            if (server.hasOutgoingWaiting(myBackupName))
                return;
            Message message = new Message(Message.Type.STILLALIVE, myName);
            server.addMessageToOutgoing(message, myBackupName);
        }
    }

    @Override
    public void processMessage(Message message) {
        if (message.getType() == Message.Type.FINISHED.getCode()) {
            NodeLogger.get().info("Task " + message.getDetails() + " finished");

            boolean expected = scheduler.taskFinished(message.getOwner(), message.getDetails());

            // optional
            Database.getInstance().storeRecord((byte[]) message.getData(), message.getDetails());
            NodeLogger.getTasking().info("DONE_" + message.getDetails() + "_" + message.getOwner());

            if (!expected)
                server.listenTo(message.getDetails());
            if (backupConnected)
                replicator.backupFinishedRecord(message.getDetails(), (byte[]) message.getData());
            else
                replicator.backupStoredRecord(message.getDetails());
        } else if (message.getType() == Message.Type.PING.getCode()) {
            if (message.getDetails() != null) {
                Database.getInstance().removeInputRecord(message.getDetails());
                markProcessing(message.getDetails());
            }
        }
    }

    private void markProcessing(String inputId) {
        processing.add(inputId);
    }

    @Override
    public void nodeConnected(String name) {
    	NodeLogger.getBoot().info("CONNECTED_" + name);
        super.nodeConnected(name);
        if (name.equals(myBackupName)) {
            // Handle connection to backup node
            backupConnected = true;
            replicator.doBackup();
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
        NodeLogger.getFailure().info("MASTER_FAIL");
    }

    @Override
    public void communicatorDown(GeneralComm comm) {
        super.communicatorDown(comm);
    }

    @Override
    public void nodeDisconnected(String name) {
        if (name.equals(myBackupName)) {
            backupConnected = false;
            replicator = new DataReplicator(this);
            replicator.backupAll();
        }
    }
}
