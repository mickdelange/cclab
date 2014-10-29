package com.cclab.core.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by ane on 10/22/14.
 */
public interface MessageInterpreter {

    public abstract void processMessage(Message message);
    public abstract void communicatorDown(GeneralComm comm);
}
