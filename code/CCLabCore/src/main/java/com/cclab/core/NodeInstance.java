package com.cclab.core;

import javax.xml.soap.Node;
import java.io.InputStream;

/**
 * Created by ane on 10/19/14.
 */
public abstract class NodeInstance {

    private NodeUtils.Server listener;

    public NodeInstance(){
        listener = new NodeUtils.Server();
        listener.start();
    }

    public abstract void processInput(NodeUtils.MessageType type, InputStream data);
}
