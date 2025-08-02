package com.github.madhavdhatrak.blaze4j;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for configuring logging in Blaze4j.
 */
public class Blaze4jLogger {
    private static final Logger LOGGER = Logger.getLogger(Blaze4jLogger.class.getName());

    /**
     * Configure global logging for all Blaze4j components with a single call.
     * This will set up both the specific loggers and the root logger to ensure
     * messages at the specified level are visible.
     * 
     * @param level The java.util.logging.Level to set (e.g., Level.INFO, Level.FINE)
     */
    public static void configureLogging(Level level) {

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);


        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        rootLogger.addHandler(consoleHandler);

        Logger packageLogger = Logger.getLogger("com.github.madhavdhatrak.blaze4j");
        packageLogger.setLevel(level);


        LOGGER.log(level, "Blaze4j logging configured at " + level.getName() + " level");
    }
} 
