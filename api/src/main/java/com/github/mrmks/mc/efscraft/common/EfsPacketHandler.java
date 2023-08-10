package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.common.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.common.packet.MessageContext;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;

public class EfsPacketHandler<EN, PL extends EN, DO extends DataOutput, DI extends DataInput, CTX> {

    private final EfsServer<EN, PL, ?, ?, DO, DI, CTX> server;
    private final MessageCodec codec;
    private final boolean autoReply;

    EfsPacketHandler(EfsServer<EN, PL, ?, ?, DO, DI, CTX> server, boolean autoReply) {
        this.server = server;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;

        codec.registerServer(PacketHello.class, new PacketHello.InternalServerHandler(event -> server.eventHandler.receive(event)));
    }

    NetworkPacket receive(PL receiver, DI dataIn) throws IOException {
        UUID uuid = server.adaptor.getEntityUUID(receiver);
        NetworkPacket packet = codec.readInput(dataIn, new MessageContext(uuid));

        if (autoReply && packet != null) {
            sendToClient(receiver, packet);
            return null;
        } else {
            return packet;
        }
    }

    void sendToClient(UUID receiver, NetworkPacket packet) {
        PL pl = server.adaptor.getPlayerEntity(server.adaptor.getPlayer(receiver));
        if (pl == null)
            return;

        sendToClient(pl, packet);
    }

    void sendToClient(PL player, NetworkPacket packet) {
        DO output = server.adaptor.createPacket();
        try {
            codec.writeOutput(packet, output);
            server.adaptor.sendPacket(Collections.singleton(player), it -> true, output);
        } catch (IOException e) {
            server.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    void sendToClient(CTX ctx, PL player, NetworkPacket packet) {

        DO output = server.adaptor.createPacket();

        try {
            codec.writeOutput(packet, output);
            server.adaptor.sendPacket(ctx, Collections.singleton(player), pl -> true, output);
        } catch (IOException e) {
            server.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    void sendToClient(CTX ctx, Collection<PL> players, Predicate<PL> test, NetworkPacket packet) {
        DO output = server.adaptor.createPacket();

        try {
            codec.writeOutput(packet, output);
            server.adaptor.sendPacket(ctx, players, test, output);
        } catch (IOException e) {
            server.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    private void closeDataOutput(DO output) {
        if (output instanceof AutoCloseable) {
            try (AutoCloseable closeable = (AutoCloseable) output) {} catch (Exception e) {
                server.logger.logWarning("Unable to close a data output", e);
            }
        }
    }

}
