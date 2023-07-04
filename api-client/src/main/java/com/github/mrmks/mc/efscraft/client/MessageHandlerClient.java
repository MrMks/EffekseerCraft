package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.math.Matrix4f;

import java.util.function.Consumer;

public final class MessageHandlerClient {
    private final RenderingQueue queue;
    private final Consumer<Runnable> scheduler;
    private transient boolean compatible = false;

    public MessageHandlerClient(RenderingQueue queue, Consumer<Runnable> scheduler) {
        this.queue = queue;
        this.scheduler = scheduler;
    }

    private <T> void syncHandle(T ins, Consumer<T> consumer) {
        scheduler.accept(() -> consumer.accept(ins));
    }

    public void handleHello(boolean flag) {
        compatible = flag;
    }

    public void handlePlayWith(SPacketPlayWith play) {
        if (compatible) syncHandle(play, this::handlePlayWith0);
    }

    public void handlePlayAt(SPacketPlayAt play) {
        if (compatible) syncHandle(play, this::handlePlayAt0);
    }

    public void handleStop(SPacketStop stop) {
        if (compatible) syncHandle(stop, this::handleStop0);
    }

    public void handleTrigger(SPacketTrigger trigger) {
        if (compatible) syncHandle(trigger, this::handleTrigger0);
    }

    public void handleClear(SPacketClear clear) {
        if (compatible) syncHandle(clear, this::handleClear0);
    }

    private void handlePlayWith0(SPacketPlayWith play) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(play.getLocalPosition())
                .rotateMC(play.getLocalRotation())
                .scale(play.getScale());

        queue.commandPlayWith(
                play.getKey(), play.getEmitter(), play.getEffect(), play.conflictOverwrite(),
                play.getFrameSkip(), play.getLifespan(), play.getDynamics(),
                base, play.getModelPosition(), play.getModelRotation(),
                play.getTarget(),
                play.followX(), play.followY(), play.followZ(), play.followYaw(), play.followPitch(),
                play.isInheritYaw(), play.isInheritPitch(),
                play.isUseHead(), play.isUseRender()
        );
    }

    private void handlePlayAt0(SPacketPlayAt play) {
        Matrix4f base = new Matrix4f().identity()
                .translatef(play.getLocalPosition())
                .rotateMC(play.getLocalRotation())
                .scale(play.getScale());

        queue.commandPlayAt(
                play.getKey(), play.getEmitter(), play.getEffect(), play.conflictOverwrite(),
                play.getFrameSkip(), play.getLifespan(), play.getDynamics(),
                base, play.getModelPosition(), play.getModelRotation(),
                play.getTargetPos(), play.getTargetRot()
        );
    }

    private void handleStop0(SPacketStop packet) {
        queue.commandStop(packet.getKey(), packet.getEmitter());
    }

    private void handleTrigger0(SPacketTrigger packet) {
        queue.commandTrigger(packet.getKey(), packet.getEmitter(), packet.getTrigger());
    }

    private void handleClear0(SPacketClear clear) {
        queue.commandClear();
    }
}
