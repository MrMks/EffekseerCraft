package com.github.mrmks.mc.efscraft.common;

public class PropertyFlags {

    public static final boolean ENABLE_LOG_DEBUG = isPropertyBoolean("efscraft.log.debug");
    public static final boolean ENABLE_CREATE_DEBUG_EFFECT = isPropertyBoolean("efscraft.effect.debug");
    public static final boolean ENABLE_TRANSPARENCY = false;

    private static boolean isPropertyBoolean(String prop) {
        return System.getProperties().containsKey(prop);
    }

}
