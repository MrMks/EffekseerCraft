package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.common.packet.MessageContext;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EfsClientPacketHandler<DI extends DataInput, DO extends DataOutput> {

    private final MessageCodec codec;
    private final boolean autoReply;
    private final EfsClient<?, ?, DI, DO> client;

    EfsClientPacketHandler(EfsClient<?,?,DI, DO> client, boolean autoReply) {
        this.client = client;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;
    }

    NetworkPacket receive(DI dataInput) throws IOException {
        NetworkPacket packet = codec.readInput(dataInput, MessageContext.AT_CLIENT);

        if (autoReply && packet != null) {
            sendToServer(packet);
            return null;
        } else {
            return packet;
        }
    }

    void sendToServer(NetworkPacket packet) {
        DO output = client.adaptor.createPacket();

        try {
            codec.writeOutput(packet, output);
            client.adaptor.sendPacket(output);
        } catch (IOException e) {
            client.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

}
