package com.github.mrmks.mc.efscraft.common.packet;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketPlayAt extends SPacketPlayAbstract {

    private Vec3f targetPos;
    private Vec2f targetRot;

    public SPacketPlayAt() {}

    public SPacketPlayAt(String key, String effect, String emitter, int lifespan, double x, double y, double z, float yaw,float pitch) {
        super(key, effect, emitter, lifespan);
        this.targetPos = new Vec3f((float) x, (float) y, (float) z);
        this.targetRot = new Vec2f(yaw, pitch);
    }

    public Vec3f getTargetPos() {
        return targetPos;
    }

    public Vec2f getTargetRot() {
        return targetRot;
    }

    static final NetworkPacket.Codec<SPacketPlayAt> CODEC = new Codec<SPacketPlayAt>() {

        @Override
        public void read(SPacketPlayAt packet, DataInput stream) throws IOException {
            super.read(packet, stream);
            packet.targetPos = new Vec3f().read(stream);
            packet.targetRot = new Vec2f().read(stream);
        }

        @Override
        public void write(SPacketPlayAt packet, DataOutput stream) throws IOException {
            super.write(packet, stream);

            if (packet.targetPos == null) packet.targetPos = new Vec3f();
            packet.targetPos.write(stream);

            if (packet.targetRot == null) packet.targetRot = new Vec2f();
            packet.targetRot.write(stream);
        }
    };
}
