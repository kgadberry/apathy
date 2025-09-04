package eu.nk2.apathy.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApathyLogger {
    private static final Logger LOGGER = LogManager.getLogger("Apathy");

    // Debug logging, only if enabled in config
    public static void debug(String format, Object... args) {
        if (ApathyLoggingConfig.isDebugEnabled()) {
            LOGGER.debug(format, args);
        }
    }

    // Info logging
    public static void info(String format, Object... args) {
        LOGGER.info(format, args);
    }
 
    // Warning logging
    public static void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    // Error logging with throwable last to match logger.error overloads
    public static void error(String format, Object... args) {
        LOGGER.error(format, args);
    }

    public static void error(String format, Throwable t, Object... args) {
        LOGGER.error(format, args, t);
    }
}
