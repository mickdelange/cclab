package com.cclab.core.utils;

import org.apache.log4j.*;
import org.apache.log4j.varia.LevelRangeFilter;

import java.io.IOException;

/**
 * Created by ane on 10/19/14.
 */
public class NodeLogger {

    private static String name = null;

    public static void configureLogger(String hostname) {
        name = "NodeLogger." + hostname;
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.DEBUG);
        ConsoleAppender consoleApp = new ConsoleAppender(new PatternLayout(
                "%-4r [%t] %-5p %c - %m%n"));
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.INFO);
        consoleApp.addFilter(filter);
        logger.addAppender(consoleApp);
        try {
            RollingFileAppender fileApp = new RollingFileAppender(
                    new PatternLayout("%d [%t] %-5p - %m%n"), "server_log_"
                    + hostname + ".txt"
            );
            fileApp.setMaxFileSize("100KB");
            logger.addAppender(fileApp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger get() {
        return Logger.getLogger(name);
    }
}
