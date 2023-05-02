package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public class SPacketPlayWith extends SPacketPlayAbstract {

    public static final byte MASK2_INHERIT_X = 0x1;
    public static final byte MASK2_INHERIT_Y = 0x2;
    public static final byte MASK2_INHERIT_Z = 0x4;
    public static final byte MASK2_INHERIT_YAW = 0x8;
    public static final byte MASK2_INHERIT_PITCH = 0x10;

    private int target;
    private byte mask2;

    public SPacketPlayWith() {}

    public SPacketPlayWith(String effect, String emitter, int lifespan, int entityId) {
        super(effect, emitter, lifespan);
        this.target = entityId;
    }

    public final int getTarget() {
        return target;
    }

    public final SPacketPlayWith markFollowX() {
        this.mask |= MASK_FOLLOW_X;
        return this;
    }

    public final SPacketPlayWith markFollowY() {
        this.mask |= MASK_FOLLOW_Y;
        return this;
    }

    public final SPacketPlayWith markFollowZ() {
        this.mask |= MASK_FOLLOW_Z;
        return this;
    }

    public final SPacketPlayWith markFollowYaw() {
        this.mask |= MASK_FOLLOW_YAW;
        return this;
    }

    public final SPacketPlayWith markFollowPitch() {
        this.mask |= MASK_FOLLOW_PITCH;
        return this;
    }

    public final boolean followX() {
        return (mask & MASK_FOLLOW_X) != 0;
    }

    public final boolean followY() {
        return (mask & MASK_FOLLOW_Y) != 0;
    }

    public final boolean followZ() {
        return (mask & MASK_FOLLOW_Z) != 0;
    }

    public final boolean followYaw() {
        return (mask & MASK_FOLLOW_YAW) != 0;
    }

    public final boolean followPitch() {
        return (mask & MASK_FOLLOW_PITCH) != 0;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        super.read(stream);
        this.target = stream.readInt();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        super.write(stream);
        stream.writeInt(this.target);
    }
}
