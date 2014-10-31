package com.cclab.core.network;

/**
 * Created by ane on 10/22/14.
 */
public interface MessageInterpreter {

    abstract void processMessage(Message message);

    abstract void communicatorDown(GeneralComm comm);
}
