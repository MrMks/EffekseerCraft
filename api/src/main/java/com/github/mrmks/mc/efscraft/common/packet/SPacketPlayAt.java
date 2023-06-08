package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketPlayAt extends SPacketPlayAbstract {

    private float[] targetPos;
    private float[] targetRot;

    public SPacketPlayAt() {}

    public SPacketPlayAt(String key, String effect, String emitter, int lifespan, double x, double y, double z) {
        this(key, effect, emitter, lifespan, x, y, z, 0, 0);
    }

    public SPacketPlayAt(String key, String effect, String emitter, int lifespan, double x, double y, double z, float yaw,float pitch) {
        super(key, effect, emitter, lifespan);
        this.targetPos = new float[] {(float) x, (float) y, (float) z};
        this.targetRot = new float[] {yaw, pitch};
    }

    public float[] getTargetPos() {
        return targetPos.clone();
    }

    public float[] getTargetRot() {
        return targetRot.clone();
    }

    static class Codec extends SPacketPlayAbstract.Codec<SPacketPlayAt> {

        public static final Codec INSTANCE = new Codec();

        @Override
        public void read(SPacketPlayAt packet, DataInput stream) throws IOException {
            super.read(packet, stream);
            packet.targetPos = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
            packet.targetRot = new float[] {stream.readFloat(), stream.readFloat()};
        }

        @Override
        public void write(SPacketPlayAt packet, DataOutput stream) throws IOException {
            super.write(packet, stream);

            if (packet.targetPos == null) packet.targetPos = new float[3];
            stream.writeFloat(packet.targetPos[0]);
            stream.writeFloat(packet.targetPos[1]);
            stream.writeFloat(packet.targetPos[2]);

            if (packet.targetRot == null) packet.targetRot = new float[2];
            stream.writeFloat(packet.targetRot[0]);
            stream.writeFloat(packet.targetRot[1]);
        }
    }
}
