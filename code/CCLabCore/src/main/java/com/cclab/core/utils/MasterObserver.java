package com.cclab.core.utils;

import com.cclab.core.BackupInstance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread that verifies if Master is still alive.
 *
 * @author Mick de Lange
 */
public class MasterObserver extends Thread {

    int interval;
    int maxTimeOut;
    long lastContact = 0;
    public boolean started = false;
    BackupInstance backupInstance;

    private boolean shouldExit = false;

    public MasterObserver(BackupInstance b) {
        backupInstance = b;
        if (!loadProperties()) {
            // Something went wrong loading properties, set to default
            interval = 2000;
            maxTimeOut = 5000;
        }
    }

    /**
     * Run the checker
     */
    public void run() {
        long currTime;
        started = true;
        try {
            while (!shouldExit) {
                Thread.sleep(interval);
                currTime = System.currentTimeMillis();

                // No message received from Master for too long, take over.
                if ((currTime - lastContact) > maxTimeOut) {
                    backupInstance.takeOver();
                }

            }
            NodeLogger.get().info("Master Observer for " + backupInstance + " has quit");
        } catch (InterruptedException e) {
            NodeLogger.get().error(e.getMessage(), e);
        }
    }

    /**
     * Register contact with Master
     */
    public void hadContact() {
        lastContact = System.currentTimeMillis();
    }

    /**
     * Get the properties from the config file.
     *
     * @return True if succeeded, False otherwise.
     */
    public boolean loadProperties() {
        Properties prop = new Properties();

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("observer.properties");

            if (inputStream != null) {
                prop.load(inputStream);
                interval = Integer.parseInt(prop.getProperty("interval"));
                maxTimeOut = Integer.parseInt(prop.getProperty("maxTimeOut"));
                return true;
            } else {
                NodeLogger.get().error("Observer properties file not found");
                return false;
            }
        } catch (IOException e) {
            NodeLogger.get().error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kill observer thread.
     */
    public void quit() {
        shouldExit = true;
        NodeLogger.get().info("OBSERVER shutting down");
    }

}
