package com.cclab.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by ane on 10/22/14.
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
            if (command != null && !interpreter.interpretAndContinue(command.split(" ")))
                break;
        }
    }
}
