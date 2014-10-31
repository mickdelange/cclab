package com.cclab.core.processing;

import com.cclab.core.utils.NodeLogger;

import java.io.*;

/**
 * Created by ane on 10/31/14.
 */
public abstract class Processor implements Runnable {
    private byte[] input = null;
    private ProcessController controller = null;
    private String taskId = null;

    public Processor(String taskId, byte[] input, ProcessController controller) {
        this.input = input;
        this.controller = controller;
        this.taskId = taskId;
    }

    @Override
    public void run() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ByteArrayInputStream inStream = new ByteArrayInputStream(input);
            process(inStream, outStream);
            controller.handleProcessorOutput(taskId, outStream.toByteArray());
        } catch (Exception e) {
            NodeLogger.get().error("Error processing image ", e);
        }
    }

    public abstract void process(InputStream input, OutputStream output) throws IOException;
}
