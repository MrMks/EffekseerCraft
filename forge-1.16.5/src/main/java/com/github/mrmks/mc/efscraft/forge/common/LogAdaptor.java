package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.common.Properties;
import org.apache.logging.log4j.Logger;

class LogAdaptor implements ILogAdaptor {

    private final boolean enableDebug = Properties.ENABLE_LOG_DEBUG;
    private final Logger logger;

    LogAdaptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void logInfo(String msg) {
        logger.info(msg);
    }

    @Override
    public void logDebug(String msg) {
        if (enableDebug)
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
