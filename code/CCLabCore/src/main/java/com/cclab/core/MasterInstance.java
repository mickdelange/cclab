package com.cclab.core;


import com.cclab.core.network.ClientComm;
import com.cclab.core.network.GeneralComm;
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
        return true;
    }

    @Override
    public void processMessage(Message message) {
        try {
            if (message.getType() == Message.Type.FINISHED.getCode()) {
                NodeLogger.get().info("Task " + message.getDetails() + " finished");
                NodeUtils.writeDataToFile((byte[]) message.getData(), "/Users/ane/Downloads/strawberry_back.jpg");

                //scheduler.taskFinished(message.getOwner());
            }
        } catch (IOException e) {
        }
    }
}
