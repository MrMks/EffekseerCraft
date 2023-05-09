package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;

// represent a temporary effect entry and later build a packet from this entry;
class PacketBuilder extends EffectEntry {
    PacketBuilder(EffectEntry entry) {
        this.effect = entry.effect;
        this.lifespan = entry.lifespan;
        this.skipFrames = entry.skipFrames;
        this.overwrite = entry.overwrite;

        this.followX = entry.followX;
        this.followY = entry.followY;
        this.followZ = entry.followZ;
        this.followYaw = entry.followYaw;
        this.followPitch = entry.followPitch;

        this.inheritYaw = entry.inheritYaw;
        this.inheritPitch = entry.inheritPitch;

        this.useHead = entry.useHead;
        this.useRender = entry.useRender;

        this.dynamic = entry.dynamic;

        this.scale = entry.scale;
        this.localPos = entry.localPos;
        this.localRot = entry.localRot;
        this.modelPos = entry.modelPos;
        this.modelRot = entry.modelRot;
    }

    PacketBuilder consumeOptions(String[] options) {
        return this;
    }

    IMessage buildPlayWith(String key, String emitter, int entityId) {
        return new SPacketPlayWith(key, effect, emitter, lifespan, entityId);
    }

    IMessage buildPlayAt(String key, String emitter, float x, float y, float z, float yaw, float pitch) {
        return new SPacketPlayAt(key, effect, emitter, lifespan, x, y, z, yaw, pitch);
    }
}