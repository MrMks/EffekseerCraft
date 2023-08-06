package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.SPacketPlayAbstract;
import com.github.mrmks.mc.efscraft.common.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.common.packet.SPacketPlayWith;

import static com.github.mrmks.mc.efscraft.common.CommandUtils.*;

// represent a temporary effect entry and later build a packet from this entry;
class PacketBuilder extends ServerRegistry {

    PacketBuilder() {
        localPos = new float[3];
        modelPos = new float[3];
        localRot = new float[2];
        modelRot = new float[2];
        scale = new float[] {1, 1, 1};
    }

    PacketBuilder(ServerRegistry entry) {
        super(entry);
    }

    PacketBuilder consumeOptions(String[] options) {
        doConsumeOptions(this, options);
        return this;
    }

    private NetworkPacket buildCommon(SPacketPlayAbstract play) {
        return play.skipFrame(skipFrames)
                .markConflictOverwrite(overwrite)
                .setLifespan(lifespan)
                .setDynamics(dynamic)
                .translateLocalTo(localPos[0], localPos[1], localPos[2])
                .translateModelTo(modelPos[0], modelPos[1], modelPos[2])
                .rotateLocalTo(localRot[0], localRot[1])
                .rotateModelTo(modelRot[0], modelRot[1])
                .scaleTo(scale[0], scale[1], scale[2]);
    }

    NetworkPacket buildPlayWith(String key, String emitter, int entityId) {
        SPacketPlayWith play = new SPacketPlayWith(key, effect, emitter, lifespan, entityId)
                .markFollowX(followArgs.followX)
                .markFollowY(followArgs.followY)
                .markFollowZ(followArgs.followZ)
                .markFollowYaw(followArgs.followYaw)
                .markFollowPitch(followArgs.followPitch)
                .markInheritYaw(followArgs.baseOnCurrentYaw)
                .markInheritPitch(followArgs.baseOnCurrentPitch)
                .markUseHead(followArgs.directionFromHead)
                .markUseRender(followArgs.directionFromBody);

        return buildCommon(play);
    }

    NetworkPacket buildPlayAt(String key, String emitter, float x, float y, float z, float yaw, float pitch) {
        SPacketPlayAt play = new SPacketPlayAt(key, effect, emitter, lifespan, x, y, z, yaw, pitch);

        return buildCommon(play);
    }
}