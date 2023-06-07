package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.packet.*;

import java.util.function.BooleanSupplier;

public final class MessageHandlerClient {
    private final RenderingQueue queue;
    private final BooleanSupplier isVersionCompatible;

    public MessageHandlerClient(BooleanSupplier boolSupplier, RenderingQueue queue) {
        this.isVersionCompatible = boolSupplier;
        this.queue = queue;
    }

    public void handlePlayWith(SPacketPlayWith play) {
        if (isVersionCompatible.getAsBoolean()) {
            handlePlay(play).playWith(
                    play.getTarget(),
                    play.followX(), play.followY(), play.followZ(), play.followYaw(), play.followPitch(),
                    play.isInheritYaw(), play.isInheritPitch(), play.isUseHead(), play.isUseRender());
        }
    }

    public void handlePlayAt(SPacketPlayAt play) {
        if (isVersionCompatible.getAsBoolean()) {
            handlePlay(play).playAt(play.getTargetPos(), play.getTargetRot());
        }

    }

    private RenderingQueue.PlayBuilder handlePlay(SPacketPlayAbstract play) {
        float[] scale = play.getScale(), pos = play.getLocalPosition(), rot = play.getLocalRotation();
        Matrix4f matrix4f = new Matrix4f().identity()
                .translatef(pos[0], pos[1], pos[2])
                .rotateMC(rot[0], rot[1])
                .scale(scale[0], scale[1], scale[2]);

        return queue.commandPlay(
                play.getKey(), play.getEffect(), play.getEmitter(),
                play.getLifespan(), play.getFrameSkip(),
                matrix4f, play.getModelPosition(), play.getModelRotation(),
                play.getDynamics(),
                play.conflictOverwrite()
        );
    }

    public void handleStop(SPacketStop stop) {
        if (isVersionCompatible.getAsBoolean())
            queue.commandStop(stop.getKey(), stop.getEmitter());

    }

    public void handleTrigger(SPacketTrigger packet) {
        if (isVersionCompatible.getAsBoolean())
            queue.commandTrigger(packet.getKey(), packet.getEmitter(), packet.getTrigger());
    }

    public void handleClear(SPacketClear clear) {
        if (isVersionCompatible.getAsBoolean())
            queue.commandClear();
    }
}
