package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.github.mrmks.mc.efscraft.common.Constants.*;

public class SPacketPlayWith extends SPacketPlayAbstract {

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
        if (flag) this.mask |= MASK_FOLLOW_X; else this.mask &= ~MASK_FOLLOW_X;
        return this;
    }

    public final SPacketPlayWith markFollowY(boolean flag) {
        if (flag) this.mask |= MASK_FOLLOW_Y; else this.mask &= ~MASK_FOLLOW_Y;
        return this;
    }

    public final SPacketPlayWith markFollowZ(boolean flag) {
        if (flag) this.mask |= MASK_FOLLOW_Z; else this.mask &= ~MASK_FOLLOW_Z;
        return this;
    }

    public final SPacketPlayWith markFollowYaw(boolean flag) {
        if (flag) this.mask |= MASK_FOLLOW_YAW; else this.mask &= ~MASK_FOLLOW_YAW;
        return this;
    }

    public final SPacketPlayWith markFollowPitch(boolean flag) {
        if (flag) this.mask |= MASK_FOLLOW_PITCH; else this.mask &= ~MASK_FOLLOW_PITCH;
        return this;
    }

    public final SPacketPlayWith markUseHead(boolean flag) {
        if (flag) this.mask |= MASK_USE_HEAD_ROTATION; else this.mask &= ~MASK_USE_HEAD_ROTATION;
        return this;
    }

    public final SPacketPlayWith markUseRender(boolean flag) {
        if (flag) this.mask |= MASK_USE_RENDER_ROTATION; else this.mask &= ~MASK_USE_RENDER_ROTATION;
        return this;
    }

    public final SPacketPlayWith markInheritYaw(boolean flag) {
        if (flag) this.mask2 |= MASK2_INHERIT_YAW; else this.mask2 &= ~MASK2_INHERIT_YAW;
        return this;
    }

    public final SPacketPlayWith markInheritPitch(boolean flag) {
        if (flag) this.mask2 |= MASK2_INHERIT_PITCH; else this.mask2 &= ~MASK2_INHERIT_PITCH;
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

    static final NetworkPacket.Codec<SPacketPlayWith> CODEC = new SPacketPlayAbstract.Codec<SPacketPlayWith>() {
        @Override
        public void read(SPacketPlayWith packet, DataInput stream) throws IOException {
            super.read(packet, stream);
            packet.target = stream.readInt();
            packet.mask2 = stream.readByte();
        }

        @Override
        public void write(SPacketPlayWith packet, DataOutput stream) throws IOException {
            super.write(packet, stream);
            stream.writeInt(packet.target);
            stream.writeByte(packet.mask2);
        }
    };
}
