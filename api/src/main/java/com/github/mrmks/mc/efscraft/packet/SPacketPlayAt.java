package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketPlayAt extends SPacketPlayAbstract {

    private float[] targetPos;
    private float[] targetRot;

    public SPacketPlayAt() {}

    public SPacketPlayAt(String effect, String emitter, int lifespan, double x, double y, double z) {
        this(effect, emitter, lifespan, x, y, z, 0, 0);
    }

    public SPacketPlayAt(String effect, String emitter, int lifespan, double x, double y, double z, float yaw,float pitch) {
        super(effect, emitter, lifespan);
        this.targetPos = new float[] {(float) x, (float) y, (float) z};
        this.targetRot = new float[] {yaw, pitch};
    }

    public float[] getModelPos() {
        return targetPos.clone();
    }

    public void read(DataInput stream) throws IOException {
        super.read(stream);
        targetPos = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
        targetRot = new float[] {stream.readFloat(), stream.readFloat()};
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        super.write(stream);

        if (targetPos == null) targetPos = new float[3];
        stream.writeFloat(targetPos[0]);
        stream.writeFloat(targetPos[1]);
        stream.writeFloat(targetPos[2]);

        if (targetRot == null) targetRot = new float[2];
        stream.writeFloat(targetRot[0]);
        stream.writeFloat(targetRot[1]);
    }
}
