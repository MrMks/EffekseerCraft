package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.util.Matrix4f;

public final class MessageHandlerClient {
    private final RenderingQueue queue;
    private transient boolean compatible = false;

    public void handleHello(boolean flag) {
        compatible = flag;
    }

    public MessageHandlerClient(RenderingQueue queue) {
        this.queue = queue;
    }

    public void handlePlayWith(SPacketPlayWith play) {
        if (compatible) {
            handlePlay(play).playWith(
                    play.getTarget(), // target
                    play.followX(), play.followY(), play.followZ(), play.followYaw(), play.followPitch(), // follows
                    play.isInheritYaw(), play.isInheritPitch(), play.isUseHead(), play.isUseRender()); // inherit and basement
        }
    }

    public void handlePlayAt(SPacketPlayAt play) {
        if (compatible) {
            handlePlay(play).playAt(play.getTargetPos(), play.getTargetRot()); // the target
        }

    }

    private RenderingQueue.PlayBuilder handlePlay(SPacketPlayAbstract play) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(play.getLocalPosition())
                .rotateMC(play.getLocalRotation())
                .scale(play.getScale());

        return queue.commandPlay(
                play.getKey(), play.getEffect(), play.getEmitter(), // identity the effect
                play.getLifespan(), play.getFrameSkip(), // frames control
                base, play.getModelPosition(), play.getModelRotation(), // generate the transform
                play.getDynamics(), // dynamic inputs
                play.conflictOverwrite() // whether overwrite the old effect if conflict happened
        );
    }

    public void handleStop(SPacketStop stop) {
        if (compatible)
            queue.commandStop(stop);
    }

    public void handleTrigger(SPacketTrigger packet) {
        if (compatible)
            queue.commandTrigger(packet);
    }

    public void handleClear(SPacketClear clear) {
        if (compatible)
            queue.commandClear();
    }
}
