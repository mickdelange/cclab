package com.cclab.core;

import com.cclab.core.data.Database;
import com.cclab.core.network.Message;
import com.cclab.core.processing.ProcessController;
import com.cclab.core.processing.Processor;
import com.cclab.core.processing.image.ImageProcessor;
import com.cclab.core.utils.NodeLogger;
import com.cclab.core.utils.NodeUtils;

public class SingleInstance extends NodeInstance implements ProcessController {
	
    boolean shouldExit = false;

    public SingleInstance() {
    	super("single");
    	run();
    }

    @Override
    public boolean extendedInterpret(String[] command) {
        try {
            if (command[0].equals("run")) {
            	run();
                return true;
            }
        } catch (Exception e) {
            NodeLogger.get().error("Error interpreting command " + NodeUtils.join(command, " ") + " (" + e.getMessage() + ")", e);
        }
        return true;
    }
    
    /**
     * Run the single processor
     */
    private void run() {
    	// Process images
    	String newInput = Database.getInstance().getNextRecordId();
        while (newInput != null && !shouldExit) {
            NodeLogger.getProcessing().info("START_" + newInput);
            
            // Get Image data
            byte[] input = Database.getInstance().getRecord(newInput);
            
            // Process
            Processor processor = new ImageProcessor(newInput, input, "blur", this);
            processor.run();
            
            // Get next
            newInput = Database.getInstance().getNextRecordId();
    	}
    }
    
    /**
     * Quit the instance
     */
    public void shutDown() {
    	shouldExit = true;
    }

	@Override
	public void processMessage(Message message) {}

	@Override
	public void handleProcessorOutput(String taskId, byte[] output) {}
}
