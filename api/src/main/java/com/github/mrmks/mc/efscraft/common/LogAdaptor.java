package com.github.mrmks.mc.efscraft.common;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LogAdaptor {

    public static LogAdaptor of(Object obj) {
        if (obj instanceof Logger)
            return new LoggerAdaptor((Logger) obj);
        else {
            try {
                if (obj instanceof org.apache.logging.log4j.Logger)
                    return new Log4JAdaptor((org.apache.logging.log4j.Logger) obj);
            } catch (NoClassDefFoundError e) {
                // do nothing here;
            }
        }

        throw new IllegalArgumentException("We can't create an adaptor for your input logger");
    }

    public abstract void logInfo(String msg);
    public abstract void logDebug(String msg);
    public abstract void logWarning(String msg);
    public abstract void logWarning(String msg, Throwable tr);

    private static class LoggerAdaptor extends LogAdaptor {

        private final Logger logger;
        private LoggerAdaptor(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void logInfo(String msg) {
            logger.info(msg);
        }

        @Override
        public void logDebug(String msg) {
            if (PropertyFlags.ENABLE_LOG_DEBUG)
                logger.info(msg);
        }

        @Override
        public void logWarning(String msg) {
            logger.warning(msg);
        }

        @Override
        public void logWarning(String msg, Throwable tr) {
            logger.log(Level.WARNING, msg, tr);
        }
    }

    private static class Log4JAdaptor extends LogAdaptor {

        private final org.apache.logging.log4j.Logger logger;
        private Log4JAdaptor(org.apache.logging.log4j.Logger logger) {
            this.logger = logger;
        }

        @Override
        public void logInfo(String msg) {
            logger.info(msg);
        }

        @Override
        public void logDebug(String msg) {
            if (PropertyFlags.ENABLE_LOG_DEBUG)
                logger.info(msg);
        }

        @Override
        public void logWarning(String msg) {
            logger.warn(msg);
        }

        @Override
        public void logWarning(String msg, Throwable tr) {
            logger.warn(msg, tr);
        }
    }
}
