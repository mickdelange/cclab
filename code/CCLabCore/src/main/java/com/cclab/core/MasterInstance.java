package com.cclab.core;


import com.cclab.core.data.Database;
import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.scheduler.Scheduler;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

import java.io.IOException;

public class MasterInstance extends NodeInstance {

    Scheduler scheduler;

    public MasterInstance(String myName, int port) throws IOException {
        super(myName);
        server = new ServerComm(port, myName, this);
        server.start();

//        scheduler = new Scheduler();
//        scheduler.run();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        try {
            if (command[0].equals("sendTo")) {
                Message message = new Message(Message.Type.get(command[1]), myName);
                message.setDetails(NodeUtils.join(command, 3, " "));
                server.addMessageToOutgoing(message, command[2]);
                return true;
            }
            if (command[0].equals("sendTaskTo")) {
                String inputId = NodeUtils.join(command, 2, " ");
                String recipient = command[1];
                sendTaskTo(recipient, inputId);
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return true;
    }

    private void sendTaskTo(String recipient, String inputId) {
        if (inputId == null || inputId.length() < 1) {
            NodeLogger.get().error("Task input identifier not supplied");
            return;
        }
        Message message = new Message(Message.Type.NEWTASK, myName);
        message.setDetails(inputId);
        byte[] input = Database.getInstance().getInput(inputId);
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

            //scheduler.taskFinished(message.getOwner());

            // optional
            Database.getInstance().storeOutput((byte[]) message.getData(), message.getDetails());
        }
    }
}
