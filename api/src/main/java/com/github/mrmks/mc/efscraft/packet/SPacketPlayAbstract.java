package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

abstract class SPacketPlayAbstract implements IMessage {
    public static final byte MASK_CONFLICT = 0x01;
    public static final byte MASK_FOLLOW_X = 0x02;
    public static final byte MASK_FOLLOW_Y = 0x04;
    public static final byte MASK_FOLLOW_Z = 0x08;
    public static final byte MASK_FOLLOW_YAW = 0x10;
    public static final byte MASK_FOLLOW_PITCH = 0x20;

    private String effect, emitter;
    private float[] rotLocal, posLocal, rotModel, scale;
    private int skip, lifespan;
    protected byte mask;

    protected SPacketPlayAbstract() {}

    protected SPacketPlayAbstract(String effect, String emitter, int lifespan) {
        this.effect = effect;
        this.emitter = emitter;
        this.lifespan = lifespan;
    }

    public final SPacketPlayAbstract rotateLocal(float yaw, float pitch) {

        return this;
    }

    public final SPacketPlayAbstract rotateModel(float yaw, float pitch) {

        return this;
    }

    public final SPacketPlayAbstract translate(float x, float y, float z) {

        return this;
    }

    public final SPacketPlayAbstract scale(float x, float y, float z) {

        return this;
    }

    public final SPacketPlayAbstract skipFrame(int len) {
        this.skip = Math.max(len, 0);

        return this;
    }

    public final SPacketPlayAbstract markConflictOverwrite() {
        this.mask |= MASK_CONFLICT;

        return this;
    }

    public String getEffect() {
        return effect;
    }

    public String getEmitter() {
        return emitter;
    }

    public int getLifespan() {
        return lifespan;
    }

    public boolean conflictOverwrite() {
        return (mask & MASK_CONFLICT) != 0;
    }

    public int getFrameSkip() {
        return skip;
    }

    public float[] getPositionLocal() {
        return posLocal;
    }

    public float[] getRotationLocal() {
        return rotLocal;
    }

    public float[] getRotationModel() {
        return rotModel;
    }

    public float[] getScale() {
        return scale;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        this.effect = stream.readUTF();
        this.emitter = stream.readUTF();
        this.lifespan = stream.readInt();
        this.skip = stream.readInt();

        this.posLocal = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
        this.rotLocal = new float[] {stream.readFloat(), stream.readFloat()};
        this.rotModel = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
        this.scale = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};

        this.mask = stream.readByte();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(effect);
        stream.writeUTF(emitter);
        stream.writeInt(lifespan);
        stream.writeInt(skip);

        stream.writeFloat(posLocal[0]);
        stream.writeFloat(posLocal[1]);
        stream.writeFloat(posLocal[2]);

        stream.writeFloat(rotLocal[0]);
        stream.writeFloat(rotLocal[1]);

        stream.writeFloat(rotModel[0]);
        stream.writeFloat(rotModel[1]);

        stream.writeFloat(scale[0]);
        stream.writeFloat(scale[1]);
        stream.writeFloat(scale[2]);

        stream.writeFloat(mask);
    }
}
