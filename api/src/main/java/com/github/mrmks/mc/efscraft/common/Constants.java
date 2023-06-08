package com.github.mrmks.mc.efscraft.common;

public class Constants {
    public static final String CHANNEL_KEY = "efscraft:main";
    public static final int PROTOCOL_VERSION = 3;

    public static final byte MASK_CONFLICT = 0x01;
    public static final byte MASK_FOLLOW_X = 0x02;
    public static final byte MASK_FOLLOW_Y = 0x04;
    public static final byte MASK_FOLLOW_Z = 0x08;
    public static final byte MASK_FOLLOW_YAW = 0x10;
    public static final byte MASK_FOLLOW_PITCH = 0x20;
    public static final byte MASK_USE_HEAD_ROTATION = 0x40;
    public static final byte MASK_USE_RENDER_ROTATION = -0x80;

    public static final byte MASK2_INHERIT_YAW = 0x1;
    public static final byte MASK2_INHERIT_PITCH = 0x2;
}
