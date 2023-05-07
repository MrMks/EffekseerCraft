package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public abstract class SPacketPlayAbstract implements IMessage {
    public static final byte MASK_CONFLICT = 0x01;

    private String key, effect, emitter;
    private float[] rotModel, posModel;
    private float[] rotLocal, posLocal;
    private float[] scale;
    private float[] dynamic;
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

    public final SPacketPlayAbstract setLifespan(int len) {
        this.lifespan = len;

        return this;
    }

    public final SPacketPlayAbstract setDynamic(int index, float value) {
        if (dynamic == null)
            dynamic = new float[index];
        else if (dynamic.length < index)
            dynamic = Arrays.copyOf(dynamic, index);

        dynamic[index - 1] = value;
        return this;
    }

    public final SPacketPlayAbstract setDynamics(float[] dynamic) {
        this.dynamic = dynamic == null ? null : dynamic.clone();

        return this;
    }

    public final SPacketPlayAbstract markConflictOverwrite(boolean flag) {
        if (flag) this.mask |= MASK_CONFLICT; else this.mask &= ~MASK_CONFLICT;
//        if (flag) this.mask |= MASK_CONFLICT;
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
        if (posLocal == null) posLocal = new float[3];
        return posLocal;
    }

    public final float[] getModelPosition() {
        if (posModel == null) posModel = new float[3];
        return posModel;
    }

    public final float[] getLocalRotation() {
        if (rotLocal == null) rotLocal = new float[2];
        return rotLocal;
    }

    public final float[] getModelRotation() {
        if (rotModel == null) rotModel = new float[2];
        return rotModel;
    }

    public final float[] getScale() {
        if (scale == null) scale = new float[] {1, 1, 1};
        return scale;
    }

    public final float[] getDynamics() {
        if (dynamic == null) dynamic = new float[0];
        return dynamic;
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

        int length = stream.readInt();
        this.dynamic = new float[length];
        for (int i = 0; i < length; i++)
            this.dynamic[i] = stream.readFloat();

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

        int length = this.dynamic == null ? 0 : this.dynamic.length;
        stream.writeInt(lifespan);
        for (int i = 0; i < length; i++)
            stream.writeFloat(this.dynamic[i]);

        stream.writeByte(mask);
    }
}
