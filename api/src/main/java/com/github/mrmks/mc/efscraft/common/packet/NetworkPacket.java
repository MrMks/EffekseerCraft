package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public interface NetworkPacket {

    interface Codec<T extends NetworkPacket> {
        default void read(T packet, DataInput stream) throws IOException {}
        default void write(T packet, DataOutput stream) throws IOException {}
    }

    @Deprecated
    interface Handler<IN extends NetworkPacket, OUT extends NetworkPacket> {
        OUT handlePacket(IN packetIn, MessageContext context);
    }

    interface ClientHandler<IN extends NetworkPacket, OUT extends NetworkPacket> {
        OUT handlePacket(IN packetIn);
    }

    interface ServerHandler<IN extends NetworkPacket, OUT extends NetworkPacket> {
        OUT handlePacket(IN packet, UUID sender);
    }
}
