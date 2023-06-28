package com.github.mrmks.mc.efscraft.common;

public interface ILogAdaptor {
    String DEBUG_PROP = "efscraft.log.debug";

    void logInfo(String msg);
    void logDebug(String msg);
    void logWarning(String msg);
    void logWarning(String msg, Throwable tr);
}
