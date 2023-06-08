package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.github.mrmks.mc.efscraft.common.Constants.MASK_CONFLICT;

public abstract class SPacketPlayAbstract implements NetworkPacket {

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

    protected static abstract class Codec<T extends SPacketPlayAbstract> implements NetworkPacket.Codec<T> {

        @Override
        public void read(SPacketPlayAbstract packet, DataInput stream) throws IOException {
            packet.key = stream.readUTF();
            packet.effect = stream.readUTF();
            packet.emitter = stream.readUTF();
            packet.lifespan = stream.readInt();
            packet.skip = stream.readInt();

            packet.scale = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
            packet.rotLocal = new float[] {stream.readFloat(), stream.readFloat()};
            packet.posLocal = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
            packet.rotModel = new float[] {stream.readFloat(), stream.readFloat()};
            packet.posModel = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};

            int length = stream.readInt();
            float[] dynamic = new float[length];
            for (int i = 0; i < length; i++)
                dynamic[i] = stream.readFloat();
            packet.dynamic = dynamic;

            packet.mask = stream.readByte();
        }

        @Override
        public void write(SPacketPlayAbstract packet, DataOutput stream) throws IOException {
            stream.writeUTF(packet.key);
            stream.writeUTF(packet.effect);
            stream.writeUTF(packet.emitter);
            stream.writeInt(packet.lifespan);
            stream.writeInt(packet.skip);

            if (packet.scale == null) packet.scale = new float[] {1, 1, 1};
            stream.writeFloat(packet.scale[0]);
            stream.writeFloat(packet.scale[1]);
            stream.writeFloat(packet.scale[2]);

            if (packet.rotLocal == null) packet.rotLocal = new float[2];
            stream.writeFloat(packet.rotLocal[0]);
            stream.writeFloat(packet.rotLocal[1]);

            if (packet.posLocal == null) packet.posLocal = new float[3];
            stream.writeFloat(packet.posLocal[0]);
            stream.writeFloat(packet.posLocal[1]);
            stream.writeFloat(packet.posLocal[2]);

            if (packet.rotModel == null) packet.rotModel = new float[2];
            stream.writeFloat(packet.rotModel[0]);
            stream.writeFloat(packet.rotModel[1]);

            if (packet.posModel == null) packet.posModel = new float[3];
            stream.writeFloat(packet.posModel[0]);
            stream.writeFloat(packet.posModel[1]);
            stream.writeFloat(packet.posModel[2]);

            int length = packet.dynamic == null ? 0 : packet.dynamic.length;
            stream.writeInt(length);
            for (int i = 0; i < length; i++)
                stream.writeFloat(packet.dynamic[i]);

            stream.writeByte(packet.mask);
        }
    }
}
