package com.cclab.core.processing;

/**
 * Interface for reacting to the termination of input processing.
 * <br/>
 * Created on 10/30/14 for CCLabCore.
 *
 * @author an3m0na
 */
public interface ProcessController {

    void handleProcessorOutput(String taskId, byte[] output);

}
