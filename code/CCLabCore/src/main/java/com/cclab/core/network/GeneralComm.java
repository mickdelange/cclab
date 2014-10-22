package com.cclab.core.network;

import com.cclab.core.utils.NodeLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ane on 10/20/14.
 */
public abstract class GeneralComm extends Thread {
    int port;
    Selector selector;
    static final int BUF_SIZE = 8192;
    public static final int DEFAULT_PORT = 9026;
    private boolean shouldExit = false;
    MessageInterpreter interpreter = null;

    public GeneralComm(int port, MessageInterpreter interpreter) {
        this.port = port;
        this.interpreter = interpreter;
    }

    @Override
    public void run() {
        try {
            // main loop
            while (true) {
                if(shouldExit)
                    break;
                checkOutgoing();
                // wait for something to happen
                selector.select();
                // iterate over the events
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    // get current event and REMOVE it from the list!!!
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        /* deactivate interest for reading */
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }

        } catch (IOException e) {
            NodeLogger.get().error(e.getMessage(), e);

        } finally {
            // cleanup
            cleanup();
            if (selector != null)
                try {
                    selector.close();
                } catch (IOException e) {
                }
        }
    }

    void writeFromQueue(SelectionKey key, ConcurrentLinkedQueue<Message> queue) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();
        ByteBuffer buf = null;
        if (queue != null && !queue.isEmpty()) {
            try {
                Message message = queue.poll();
                byte[] data = message.toBytes();
                NodeLogger.get().info("Sending " + message);
                buf = ByteBuffer.allocateDirect(data.length + 8);
                buf.putInt(data.length);
                buf.put(data);
                buf.flip();
                channel.write(buf);
            } catch (IOException e) {
                if (buf != null)
                    buf.clear();
                channel.close();
                return;
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    abstract void checkOutgoing();

    abstract void accept(SelectionKey key) throws IOException;

    abstract void write(SelectionKey key) throws IOException;

    abstract void read(SelectionKey key) throws IOException;

    abstract void cleanup();

    abstract void initialize() throws IOException;

    public void quit(){
        shouldExit = true;
        selector.wakeup();
    }
}
