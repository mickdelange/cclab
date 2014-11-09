package com.cclab.core;

import com.cclab.core.data.Database;
import com.cclab.core.network.ClientComm;
import com.cclab.core.network.Message;
import com.cclab.core.redundancy.MasterObserver;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;
import java.util.Map;

public class BackupInstance extends NodeInstance {

    int port;
    String masterIP = null;
    String myMasterName;
    MasterObserver masterObserver;

    public BackupInstance(String myName, String masterName, String masterIP, int port) throws IOException {
        super(myName);

        NodeLogger.getFailure().info("BACKUP_BOOT");
        myMasterName = masterName;
        this.masterIP = masterIP;
        this.port = port;

        //start listening to master
        Database.isBackup = true;
        ClientComm client = new ClientComm(masterIP, port, myName, this);
        client.start();
        clients.put(masterIP, client);

        masterObserver = new MasterObserver(this);

        if (NodeUtils.testModeOn) {
            try {
                AwsConnect.init();
            } catch (Exception e) {
                NodeLogger.get().error(e.getMessage(), e);
            }
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
            broadcastMasterSwitch();

//            new BootObserver(myMasterName, BootSettings.backup(myMasterName, myName, myIP));

            // Kill self
            safeShutDown();
        } catch (IOException e) {
            NodeLogger.get().error("Could not start master, backup failed");
            NodeLogger.get().error(e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Notify all registered Worker nodes of new master.
     */
    private void broadcastMasterSwitch() {
        String ownIP = NodeUtils.testModeOn ? "localhost" : AwsConnect.getInstancePrivIP(myName);
        Message notification = new Message(Message.Type.NEWMASTER, myName);
        notification.setDetails(ownIP);

        for (Map.Entry<String, ClientComm> client : clients.entrySet()) {
            client.getValue().addMessageToOutgoing(notification);
        }
    }

    /**
     * Register to a node.
     *
     * @param nodeName
     */
    private void registerNode(String nodeName) {
        String nodeIP = NodeUtils.testModeOn ? "localhost" : AwsConnect.getInstancePrivIP(nodeName);
        try {
            ClientComm client = new ClientComm(nodeIP, NodeUtils.testModeOn ? 9030 : port, myName, this);
            client.start();
            clients.put(nodeIP, client);
        } catch (IOException e) {
            e.printStackTrace();
            NodeLogger.get().error("Could not register node.");
        }
    }

    private void safeShutDown() {
        boolean done;
        NodeLogger.get().info("Waiting for nodes to receive switch notification");
        do {
            done = true;
            // check if all notifications to workers have been sent
            for (Map.Entry<String, ClientComm> client : clients.entrySet())
//                if (!client.getKey().equals(masterIP))
                if (client.getValue().hasOutgoingWaiting()) {
                    done = false;
                    break;
                }
        } while (!done);
        shutDown();
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
            new Thread(masterObserver).start();

        if (message.getType() == Message.Type.STILLALIVE.getCode()) {
            NodeLogger.get().debug("MASTER is healthy");
        } else if (message.getType() == Message.Type.BACKUPTASK.getCode()) {
            // Store new image in input
            Database.getInstance().storeInputRecord((byte[]) message.getData(), message.getDetails());
        } else if (message.getType() == Message.Type.BACKUPFIN.getCode()) {
            Database.getInstance().removeInputRecord(message.getDetails());
            Database.getInstance().storeRecord((byte[]) message.getData(), message.getDetails());
            // Move image from input to output
        } else if (message.getType() == Message.Type.BACKUPCONNECT.getCode()) {
            registerNode(message.getDetails());
        } else {
            NodeLogger.get().error("BACKUP received unknown message.");
        }
    }

    @Override
    public void nodeConnected(String name) {
        super.nodeConnected(name);
    }

    @Override
    public void shutDown() {
        masterObserver.quit();
        super.shutDown();
        NodeLogger.get().info("BACKUP shutting down");
        NodeLogger.getFailure().info("BACKUP_FAIL");
    }
}
