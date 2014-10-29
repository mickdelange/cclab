package com.cclab.core.utils;

import org.apache.log4j.*;
import org.apache.log4j.varia.LevelRangeFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by ane on 10/19/14.
 */
public class NodeLogger {

    private static String nodeName = null;

    public static void configureLogger(String name, Object caller) {
        nodeName = name;

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
        return Logger.getLogger(nodeName);
    }
}
