package com.cclab.core.processing;

/**
 * Created by ane on 10/30/14.
 */
public interface ProcessController {

    void handleProcessorOutput(String taskId, byte[] output);

}
