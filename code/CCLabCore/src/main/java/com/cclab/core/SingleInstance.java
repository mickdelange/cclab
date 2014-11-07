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
    	NodeLogger.get().info("Started single instance.");
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
            NodeLogger.get().info("Started processing: " + newInput);

            NodeLogger.getTasking().info("ASSIGN_" + newInput + "_single");
            // Get Image data
            byte[] input = Database.getInstance().getRecord(newInput);
            
            // Process
            NodeLogger.getProcessing().info("START_" + newInput);
            Processor processor = new ImageProcessor(newInput, input, "blur", this);
            processor.run();
            NodeLogger.getProcessing().info("FINISH_" + newInput);

            NodeLogger.get().info("Finished processing: " + newInput);
            
            // Get next
            newInput = Database.getInstance().getNextRecordId();
    	}
        
    	NodeLogger.get().info("Finished processing all images.");
    	shutDown();
    }
    
    /**
     * Quit the instance
     */
    @Override
    public void shutDown() {
    	super.shutDown();
    	shouldExit = true;
        System.exit(1);
    }

	@Override
	public void processMessage(Message message) {}

	@Override
	public void handleProcessorOutput(String taskId, byte[] output) {
		Database.getInstance().storeRecord(output, taskId);
		NodeLogger.getTasking().info("DONE_" + taskId + "_single");
	}
}
