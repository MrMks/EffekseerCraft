package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAbstract;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;

import static com.github.mrmks.mc.efscraft.CommandUtils.*;

// represent a temporary effect entry and later build a packet from this entry;
class PacketBuilder extends EffectEntry {

    PacketBuilder() {
        localPos = new float[3];
        modelPos = new float[3];
        localRot = new float[2];
        modelRot = new float[2];
        scale = new float[] {1, 1, 1};
    }

    PacketBuilder(EffectEntry entry) {
        super(entry);
    }

    PacketBuilder consumeOptions(String[] options) {
        doConsumeOptions(this, options);
        return this;
    }

    private IMessage buildCommon(SPacketPlayAbstract play) {
        return play.skipFrame(skipFrames)
                .markConflictOverwrite(overwrite)
                .setDynamics(dynamic)
                .translateLocalTo(localPos[0], localPos[1], localPos[2])
                .translateModelTo(modelPos[0], modelPos[1], modelPos[2])
                .rotateLocalTo(localRot[0], localRot[1])
                .rotateModelTo(modelRot[0], modelRot[1])
                .scaleTo(scale[0], scale[1], scale[2]);
    }

    IMessage buildPlayWith(String key, String emitter, int entityId) {
        SPacketPlayWith play = new SPacketPlayWith(key, effect, emitter, lifespan, entityId)
                .markFollowX(followX)
                .markFollowY(followY)
                .markFollowZ(followZ)
                .markFollowYaw(followYaw)
                .markFollowPitch(followPitch)
                .markInheritYaw(inheritYaw)
                .markInheritPitch(inheritPitch)
                .markUseHead(useHead)
                .markUseRender(useRender);

        return buildCommon(play);
    }

    IMessage buildPlayAt(String key, String emitter, float x, float y, float z, float yaw, float pitch) {
        SPacketPlayAt play = new SPacketPlayAt(key, effect, emitter, lifespan, x, y, z, yaw, pitch);

        return buildCommon(play);
    }
}