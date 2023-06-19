package com.github.mrmks.mc.efscraft;

public interface ILogAdaptor {
    String DEBUG_PROP = "efscraft.log.debug";

    void logInfo(String msg);
    void logDebug(String msg);
    void logWarning(String msg);
    void logWarning(String msg, Throwable tr);
}
