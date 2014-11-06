package com.cclab.core.utils;

import org.apache.log4j.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Universal logger provider.
 * <p/>
 * The properties of the logger are read by default from the log4j.properties
 * file that should be located under the root class directory. The name of the
 * file appender is changed to the name of the current process that is provided
 * in the constructor.
 * <p/>
 * Created on 10/19/14 for CCLabCore.
 *
 * @author an3m0na
 */
public class NodeLogger {

    public static void configureLogger(String name, Object caller) {

        Properties props = new Properties();
        try {
            InputStream configStream = caller.getClass().getResourceAsStream("/log4j.properties");
            props.load(configStream);
            configStream.close();
        } catch(IOException e) {
            System.out.println("Error: Cannot load log configuration file");
            return;
        }

        props.setProperty("log.name",name);
        LogManager.resetConfiguration();
        PropertyConfigurator.configure(props);
    }

    public static Logger get() {
        return Logger.getLogger("full");
    }

    public static Logger getProcessing() {
        return Logger.getLogger("monitor.processing");
    }

    public static Logger getTasking() {
        return Logger.getLogger("monitor.tasking");
    }
}
