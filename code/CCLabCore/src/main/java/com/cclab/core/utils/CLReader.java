package com.cclab.core.utils;

import javax.xml.soap.Node;
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

    public CLReader(CLInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public void run() {
        String command = null;
        InputStreamReader inStream = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inStream);
        while (true) {
            // see if cli command was given
            try {
                command = reader.readLine();
            } catch (IOException e) {
                NodeLogger.get().error(e.getMessage(), e);
            }
            // received command in CLI
            NodeLogger.get().debug("Received command " + command + ".");
            if (command != null && !interpreter.interpretAndContinue(command.split(" ")))
                break;
        }
    }
}
