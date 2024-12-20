package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.common.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

class LogAdaptor implements ILogAdaptor {

    private final boolean enableDebug = Properties.ENABLE_LOG_DEBUG;
    private final Logger logger;

    LogAdaptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void logInfo(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void logDebug(String msg) {
        if (enableDebug)
            logger.log(Level.INFO, msg);
    }

    @Override
    public void logWarning(String msg) {
        logger.log(Level.WARNING, msg);
    }

    @Override
    public void logWarning(String msg, Throwable tr) {
        logger.log(Level.WARNING, msg, tr);
    }
}
