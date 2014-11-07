package com.cclab.core.network;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created on 11/7/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class ServerReceiver extends Thread {
    public ConcurrentLinkedQueue<SelectionKey> queue;
    private DataReceiver receiver = null;
    private ServerComm communicator = null;

    public ServerReceiver(ServerComm communicator) {
        this.queue = new ConcurrentLinkedQueue<SelectionKey>();
        this.communicator = communicator;
    }


    @Override
    public void run() {
        synchronized (this) {
            SelectionKey key = queue.poll();
            if (key != null) {
                if (receiver != null && !receiver.isDone())
                    receiver.doReceive();
                else {
                    receiver = new DataReceiver(key, communicator);
                    receiver.doReceive();
                }
            }
        }
    }
}
