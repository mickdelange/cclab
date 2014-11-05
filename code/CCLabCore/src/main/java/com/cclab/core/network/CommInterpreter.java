package com.cclab.core.network;

/**
 * Interface for reacting to events reported by a communicator
 * <br/>
 * Created on 10/22/14 for CCLabCore.
 *
 * @author an3m0na
 */
public interface CommInterpreter {

    abstract void processMessage(Message message);

    abstract void nodeConnected(String name);

    abstract void communicatorDown(GeneralComm comm);
}
