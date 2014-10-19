package com.cclab.core;

import com.cclab.core.processing.ImageProcessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ane on 10/15/14.
 */
public class WorkerInstance{

    public WorkerInstance() {

        //super();

        try {
            NodeUtils.sendfile("localhost", NodeUtils.MessageType.LOADOUTPUT, "/Users/ane/Downloads/strawberry_small.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processInput(InputStream in) throws IOException {
        ImageProcessor.process(in, new ByteArrayOutputStream(), "thumbnail,blur");

    }
}
