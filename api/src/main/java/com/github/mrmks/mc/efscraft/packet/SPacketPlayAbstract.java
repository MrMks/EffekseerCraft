package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class SPacketPlayAbstract implements IMessage {
    public static final byte MASK_CONFLICT = 0x01;
    public static final byte MASK_FOLLOW_X = 0x02;
    public static final byte MASK_FOLLOW_Y = 0x04;
    public static final byte MASK_FOLLOW_Z = 0x08;
    public static final byte MASK_FOLLOW_YAW = 0x10;
    public static final byte MASK_FOLLOW_PITCH = 0x20;

    private String key, effect, emitter;
    private float[] rotModel, posModel;
    private float[] rotLocal, posLocal;
    private float[] scale;
    private int skip, lifespan;
    protected byte mask;

    protected SPacketPlayAbstract() {}

    protected SPacketPlayAbstract(String key, String effect, String emitter, int lifespan) {
        this.key = key;
        this.effect = effect;
        this.emitter = emitter;
        this.lifespan = lifespan;
    }

    public final SPacketPlayAbstract rotateLocalTo(float yaw, float pitch) {
        if (rotLocal == null || rotLocal.length < 2)
            rotLocal = new float[] {yaw, pitch};
        else {
            rotLocal[0] = yaw;
            rotLocal[1] = pitch;
        }
        return this;
    }

    public final SPacketPlayAbstract rotateModelTo(float yaw, float pitch) {
        if (rotModel == null || rotModel.length < 2)
            rotModel = new float[] {yaw, pitch};
        else {
            rotModel[0] = yaw;
            rotModel[1] = pitch;
        }
        return this;
    }

    public final SPacketPlayAbstract translateLocalTo(float x, float y, float z) {
        if (posLocal == null || posLocal.length < 3)
            posLocal = new float[] {x, y, z};
        else {
            posLocal[0] = x;
            posLocal[1] = y;
            posLocal[2] = z;
        }
        return this;
    }

    public final SPacketPlayAbstract translateModelTo(float x, float y, float z) {
        if (posModel == null || posModel.length < 3)
            posModel = new float[] {x, y, z};
        else {
            posModel[0] = x;
            posModel[1] = y;
            posModel[2] = z;
        }

        return this;
    }

    public final SPacketPlayAbstract scaleTo(float x, float y, float z) {
        if (scale == null || scale.length < 3)
            scale = new float[] {x, y, z};
        else {
            scale[0] = x;
            scale[1] = y;
            scale[2] = z;
        }
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

    public String getKey() {
        return key;
    }

    public final String getEffect() {
        return effect;
    }

    public final String getEmitter() {
        return emitter;
    }

    public final int getLifespan() {
        return lifespan;
    }

    public final boolean conflictOverwrite() {
        return (mask & MASK_CONFLICT) != 0;
    }

    public final int getFrameSkip() {
        return skip;
    }

    public final float[] getLocalPosition() {
        return posLocal;
    }

    public final float[] getModelPosition() {
        return posModel;
    }

    public final float[] getLocalRotation() {
        return rotLocal;
    }

    public final float[] getModelRotation() {
        return rotModel;
    }

    public final float[] getScale() {
        return scale;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        this.key = stream.readUTF();
        this.effect = stream.readUTF();
        this.emitter = stream.readUTF();
        this.lifespan = stream.readInt();
        this.skip = stream.readInt();

        this.scale = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
        this.rotLocal = new float[] {stream.readFloat(), stream.readFloat()};
        this.posLocal = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
        this.rotModel = new float[] {stream.readFloat(), stream.readFloat()};
        this.posModel = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};

        this.mask = stream.readByte();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(key);
        stream.writeUTF(effect);
        stream.writeUTF(emitter);
        stream.writeInt(lifespan);
        stream.writeInt(skip);

        if (scale == null) scale = new float[] {1, 1, 1};
        stream.writeFloat(scale[0]);
        stream.writeFloat(scale[1]);
        stream.writeFloat(scale[2]);

        if (rotLocal == null) rotLocal = new float[2];
        stream.writeFloat(rotLocal[0]);
        stream.writeFloat(rotLocal[1]);

        if (posLocal == null) posLocal = new float[3];
        stream.writeFloat(posLocal[0]);
        stream.writeFloat(posLocal[1]);
        stream.writeFloat(posLocal[2]);

        if (rotModel == null) rotModel = new float[2];
        stream.writeFloat(rotModel[0]);
        stream.writeFloat(rotModel[1]);

        if (posModel == null) posModel = new float[3];
        stream.writeFloat(posModel[0]);
        stream.writeFloat(posModel[1]);
        stream.writeFloat(posModel[2]);

        stream.writeByte(mask);
    }
}
