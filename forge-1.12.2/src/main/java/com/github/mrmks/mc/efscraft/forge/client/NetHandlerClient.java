package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.packet.*;

class NetHandlerClient {
    private final RenderQueue queue;
    private final ClientProxy proxy;


    NetHandlerClient(ClientProxy proxy, RenderQueue queue) {
        this.proxy = proxy;
        this.queue = queue;
    }

    IMessage handlePlayWith(SPacketPlayWith play, MessageContext context) {
        if (proxy.isVersionCompatible()) {
            RenderQueue.Controller controller = new RenderQueue.ControllerWith(
                    play.getTarget(),
                    play.followX(), play.followY(), play.followZ(), play.followYaw(), play.followPitch(),
                    play.isInheritYaw(), play.isInheritPitch(), play.isUseHead(), play.isUseRender()
            );

            handlePlay(play, controller);
        }

        return null;
    }

    IMessage handlePlayAt(SPacketPlayAt play, MessageContext context) {
        if (proxy.isVersionCompatible()) {
            RenderQueue.Controller controller = new RenderQueue.ControllerAt(
                    play.getTargetPos(), play.getTargetRot()
            );

            handlePlay(play, controller);
        }

        return null;
    }

    private void handlePlay(SPacketPlayAbstract play, RenderQueue.Controller controller) {
        float[] scale = play.getScale(), pos = play.getLocalPosition(), rot = play.getLocalRotation();
        Matrix4f matrix4f = new Matrix4f().identity()
                .translatef(pos[0], pos[1], pos[2])
                .rotateMC(rot[0], rot[1])
                .scale(scale[0], scale[1], scale[2]);

        queue.commandPlay(
                play.getKey(), play.getEffect(), play.getEmitter(),
                play.getLifespan(), play.getFrameSkip(),
                matrix4f, play.getModelPosition(), play.getModelRotation(),
                play.getDynamics(),
                controller,
                play.conflictOverwrite()
        );
    }

    IMessage handleStop(SPacketStop stop, MessageContext context) {
        if (proxy.isVersionCompatible())
            queue.commandStop(stop.getKey(), stop.getEmitter());

        return null;
    }

    IMessage handleClear(SPacketClear clear, MessageContext context) {
        if (proxy.isVersionCompatible())
            queue.commandClear();

        return null;
    }

}
