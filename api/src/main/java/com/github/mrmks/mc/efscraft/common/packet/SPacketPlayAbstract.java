package com.github.mrmks.mc.efscraft.common.packet;

import com.github.mrmks.mc.efscraft.util.Vec3f;
import com.github.mrmks.mc.efscraft.util.Vec2f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.github.mrmks.mc.efscraft.common.Constants.MASK_CONFLICT;

public abstract class SPacketPlayAbstract implements NetworkPacket {

    private String key, effect, emitter;
    private Vec2f rotModel, rotLocal;
    private Vec3f posModel, posLocal;
    private Vec3f scale;
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
        rotLocal = new Vec2f(yaw, pitch);

        return this;
    }

    public final SPacketPlayAbstract rotateModelTo(float yaw, float pitch) {
        rotModel = new Vec2f(yaw, pitch);

        return this;
    }

    public final SPacketPlayAbstract translateLocalTo(float x, float y, float z) {
        posLocal = new Vec3f(x, y, z);

        return this;
    }

    public final SPacketPlayAbstract translateModelTo(float x, float y, float z) {
        posModel = new Vec3f(x, y, z);

        return this;
    }

    public final SPacketPlayAbstract scaleTo(float x, float y, float z) {
        scale = new Vec3f(x, y, z);

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

    public final Vec3f getLocalPosition() {
        if (posLocal == null) posLocal = new Vec3f();
        return posLocal;
    }

    public final Vec3f getModelPosition() {
        if (posModel == null) posModel = new Vec3f();
        return posModel;
    }

    public final Vec2f getLocalRotation() {
        if (rotLocal == null) rotLocal = new Vec2f();
        return rotLocal;
    }

    public final Vec2f getModelRotation() {
        if (rotModel == null) rotModel = new Vec2f();
        return rotModel;
    }

    public final Vec3f getScale() {
        if (scale == null) scale = new Vec3f(1, 1, 1);
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

            packet.scale = new Vec3f().read(stream);
            packet.rotLocal = new Vec2f().read(stream);
            packet.posLocal = new Vec3f().read(stream);
            packet.rotModel = new Vec2f().read(stream);
            packet.posModel = new Vec3f().read(stream);

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

            if (packet.scale == null) packet.scale = new Vec3f(1, 1, 1);
            packet.scale.write(stream);

            if (packet.rotLocal == null) packet.rotLocal = new Vec2f();
            packet.rotLocal.write(stream);

            if (packet.posLocal == null) packet.posLocal = new Vec3f();
            packet.posLocal.write(stream);

            if (packet.rotModel == null) packet.rotModel = new Vec2f();
            packet.rotModel.write(stream);

            if (packet.posModel == null) packet.posModel = new Vec3f();
            packet.posModel.write(stream);

            int length = packet.dynamic == null ? 0 : packet.dynamic.length;
            stream.writeInt(length);
            for (int i = 0; i < length; i++)
                stream.writeFloat(packet.dynamic[i]);

            stream.writeByte(packet.mask);
        }
    }
}
