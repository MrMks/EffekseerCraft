package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.math.Matrix4f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class EfsClientPacketHandler<DI extends DataInput, DO extends DataOutput> {

    private final MessageCodec codec;
    private final boolean autoReply;
    private final EfsClient<?, ?, DI, DO> client;
    private final EfsDrawingQueue<?> queue;

    EfsClientPacketHandler(EfsClient<?,?,DI, DO> client, boolean autoReply, EfsDrawingQueue<?> queue) {
        this.client = client;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;
        this.queue = queue;

        codec.registerClient(PacketHello.class, new PacketHello.ClientHandler(flag -> client.adaptor.schedule(() -> client.compatible = flag)));
        codec.registerClient(SPacketPlayWith.class, this::handlePlayWith);
        codec.registerClient(SPacketPlayAt.class, this::handlePlayAt);
        codec.registerClient(SPacketTrigger.class, this::handleTrigger);
        codec.registerClient(SPacketStop.class, this::handleStop);
        codec.registerClient(SPacketClear.class, this::handleClear);
    }

    DO receive(DI dataInput) throws IOException {
        NetworkPacket packet = codec.readInput(dataInput, MessageContext.AT_CLIENT);

        if (autoReply && packet != null) {
            sendToServer(packet);
            return null;
        } else {
            return writePacketOutput(packet);
        }
    }

    void sendToServer(NetworkPacket packet) {
        try {
            DO output = writePacketOutput(packet);
            if (output != null) {
                client.adaptor.sendPacket(output);
                client.adaptor.closeOutput(output);
            }
        } catch (IOException e) {
            client.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    private DO writePacketOutput(NetworkPacket packet) throws IOException {
        if (packet != null) {
            DO output = client.adaptor.createOutput();
            if (codec.writeOutput(packet, output)) {
                return output;
            }

            client.adaptor.closeOutput(output);
        }

        return null;
    }

    private void handlePlayWith(SPacketPlayWith packet) { client.adaptor.schedule(() -> handlePlayWith0(packet)); }
    private void handlePlayWith0(SPacketPlayWith packet) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(packet.getLocalPosition())
                .rotateMC(packet.getLocalRotation())
                .scale(packet.getScale());


        queue.commandPlayWith(packet.getKey(), packet.getEmitter(), packet.getEffect(), packet.conflictOverwrite(),
                packet.getFrameSkip(), packet.getLifespan(), packet.getDynamics(),
                base, packet.getModelPosition(), packet.getModelRotation(),
                packet.getTarget(),
                packet.followX(), packet.followY(), packet.followZ(), packet.followYaw(), packet.followPitch(),
                packet.isInheritYaw(), packet.isInheritPitch(),
                packet.isUseHead(), packet.isUseRender());
    }

    private void handlePlayAt(SPacketPlayAt packet) { client.adaptor.schedule(() -> handlePlayAt0(packet)); }
    private void handlePlayAt0(SPacketPlayAt packet) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(packet.getLocalPosition())
                .rotateMC(packet.getLocalRotation())
                .scale(packet.getScale());

        queue.commandPlayAt(
                packet.getKey(), packet.getEmitter(), packet.getEffect(), packet.conflictOverwrite(),
                packet.getFrameSkip(), packet.getLifespan(), packet.getDynamics(),
                base, packet.getModelPosition(), packet.getModelRotation(),
                packet.getTargetPos(), packet.getTargetRot()
        );
    }

    private void handleTrigger(SPacketTrigger packet) { client.adaptor.schedule(() -> handleTrigger0(packet)); }
    private void handleTrigger0(SPacketTrigger packet) {
        queue.commandTrigger(packet.getKey(), packet.getEmitter(), packet.getTrigger());
    }

    private void handleStop(SPacketStop packet) { client.adaptor.schedule(() -> handleStop0(packet)); }
    private void handleStop0(SPacketStop packet) {
        queue.commandStop(packet.getKey(), packet.getEmitter());
    }

    private void handleClear(SPacketClear packet) { client.adaptor.schedule(() -> handleClear0(packet)); }
    private void handleClear0(SPacketClear packet) {
        queue.commandClear();
    }
}
