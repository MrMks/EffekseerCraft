package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketPlayWith extends SPacketPlayAbstract {

    public static final byte MASK_FOLLOW_X = 0x02;
    public static final byte MASK_FOLLOW_Y = 0x04;
    public static final byte MASK_FOLLOW_Z = 0x08;
    public static final byte MASK_FOLLOW_YAW = 0x10;
    public static final byte MASK_FOLLOW_PITCH = 0x20;
    public static final byte MASK_USE_HEAD_ROTATION = 0x40;
    public static final byte MASK_USE_RENDER_ROTATION = -0x80;

    public static final byte MASK2_INHERIT_YAW = 0x1;
    public static final byte MASK2_INHERIT_PITCH = 0x2;

    private int target;
    private byte mask2;

    public SPacketPlayWith() {}

    public SPacketPlayWith(String key, String effect, String emitter, int lifespan, int entityId) {
        super(key, effect, emitter, lifespan);
        this.target = entityId;
    }

    public final int getTarget() {
        return target;
    }

    public final SPacketPlayWith markFollowX(boolean flag) {
        this.mask |= flag ? MASK_FOLLOW_X : ~MASK_FOLLOW_X;
        return this;
    }

    public final SPacketPlayWith markFollowY(boolean flag) {
        this.mask |= flag ? MASK_FOLLOW_Y : ~MASK_FOLLOW_Y;
        return this;
    }

    public final SPacketPlayWith markFollowZ(boolean flag) {
        this.mask |= flag ? MASK_FOLLOW_Z : ~MASK_FOLLOW_Z;
        return this;
    }

    public final SPacketPlayWith markFollowYaw(boolean flag) {
        this.mask |= flag ? MASK_FOLLOW_YAW : ~MASK_FOLLOW_YAW;
        return this;
    }

    public final SPacketPlayWith markFollowPitch(boolean flag) {
        this.mask |= flag ? MASK_FOLLOW_PITCH : ~MASK_FOLLOW_PITCH;
        return this;
    }

    public final SPacketPlayWith markUseHead(boolean flag) {
        this.mask |= flag ? MASK_USE_HEAD_ROTATION : ~MASK_USE_HEAD_ROTATION;
        return this;
    }

    public final SPacketPlayWith markUseRender(boolean flag) {
        this.mask |= flag ? MASK_USE_RENDER_ROTATION : ~MASK_USE_RENDER_ROTATION;
        return this;
    }

    public final SPacketPlayWith markInheritYaw(boolean flag) {
        this.mask2 |= flag ? MASK2_INHERIT_YAW : ~MASK2_INHERIT_YAW;
        return this;
    }

    public final SPacketPlayWith markInheritPitch(boolean flag) {
        this.mask2 |= flag ? MASK2_INHERIT_PITCH : ~MASK2_INHERIT_PITCH;
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

    public final boolean isUseHead() {
        return (mask & MASK_USE_HEAD_ROTATION) != 0;
    }

    public final boolean isUseRender() {
        return (mask & MASK_USE_RENDER_ROTATION) != 0;
    }

    public final boolean isInheritYaw() {
        return (mask2 & MASK2_INHERIT_YAW) != 0;
    }

    public final boolean isInheritPitch() {
        return (mask2 & MASK2_INHERIT_PITCH) != 0;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        super.read(stream);
        this.target = stream.readInt();
        this.mask2 = stream.readByte();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        super.write(stream);
        stream.writeInt(this.target);
        stream.writeByte(this.mask2);
    }
}
