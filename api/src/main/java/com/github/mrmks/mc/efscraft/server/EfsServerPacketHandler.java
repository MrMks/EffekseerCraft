package com.github.mrmks.mc.efscraft.server;

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

class EfsServerPacketHandler<SV, EN, PL extends EN, DI extends DataInput, DO extends DataOutput> {

    private final EfsServer<SV, ?, EN, PL, ?, DI, DO> server;
    private final MessageCodec codec;
    private final boolean autoReply;

    EfsServerPacketHandler(EfsServer<SV, ?, EN, PL, ?, DI, DO> server, boolean autoReply) {
        this.server = server;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;
    }

    void init() {
        codec.registerServer(PacketHello.class, new PacketHello.InternalServerHandler(server.eventHandler::receive));
    }

    DO receive(SV sv, PL receiver, DI dataIn) throws IOException {
        UUID uuid = server.adaptor.getEntityUUID(receiver);
        NetworkPacket packet = codec.readInput(dataIn, new MessageContext(uuid));

        if (autoReply && packet != null) {
            sendToClient(sv, receiver, packet);
            return null;
        } else {
            return writePacketOutput(packet);
        }
    }

    void sendToClient(SV sv, UUID receiver, NetworkPacket packet) {
        PL pl = server.adaptor.getPlayer(sv, receiver);
        if (pl == null)
            return;

        sendToClient(sv, pl, packet);
    }

    void sendToClient(SV svr, PL player, NetworkPacket packet) {
        sendToClient(svr, Collections.singleton(player), any -> true, packet);
    }

    void sendToClient(SV svr, Collection<PL> players, Predicate<PL> test, NetworkPacket packet) {
        try {
            DO output = writePacketOutput(packet);
            if (output != null) {
                server.adaptor.sendPacket(svr, players, test, output);
                server.adaptor.closeOutput(output);
            }
        } catch (IOException e) {
            server.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    private DO writePacketOutput(NetworkPacket packet) throws IOException {
        if (packet != null) {
            DO output = server.adaptor.createOutput();
            if (codec.writeOutput(packet, output)) {
                return output;
            }
            server.adaptor.closeOutput(output);
        }

        return null;
    }

}
