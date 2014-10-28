package com.cclab.core;


import com.cclab.core.network.Message;
import com.cclab.core.network.ServerComm;
import com.cclab.core.scheduler.Scheduler;

import java.io.IOException;

public class MasterInstance extends NodeInstance {
	
	Scheduler scheduler;

    public MasterInstance(String myName, int port) throws IOException {
        super(myName);
        server = new ServerComm(port, this);
        server.start();
        
        scheduler = new Scheduler(myName);
        scheduler.run();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        return true;
    }

	@Override
	public void processMessage(Message message) throws IOException {
		if (message.getType() == Message.Type.FINISHED.getCode()) {
			scheduler.taskFinished(message.getOwner());
		}
	}
}
