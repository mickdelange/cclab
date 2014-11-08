package com.cclab.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Thread responsible for reading command line instructions.
 * <p/>
 * The constructor requires an interpreter to report the instruction to. If the
 * result of the interpretation is false, the reader shuts down.
 * <p/>
 * Created on 10/22/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class CLReader extends Thread {

    CLInterpreter interpreter;
    private boolean shouldExit = false;

    public CLReader(CLInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public void run() {
        String command = null;
        BufferedInputStream inStream = new BufferedInputStream(System.in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        while (!shouldExit) {
            // see if cli command was given
            command = null;
            try {
                if (inStream.available() > 0)
                    command = reader.readLine();
            } catch (IOException e) {
                NodeLogger.get().error(e.getMessage(), e);
            }
            // received command in CLI

            if (command != null) {
                NodeLogger.get().debug("Received command " + command + ".");
                interpreter.interpretCommand(command.split(" "));
            }
        }
        NodeLogger.get().info("CL Reader for " + interpreter + " has quit");
    }

    public void quit() {
        shouldExit = true;
    }
}
