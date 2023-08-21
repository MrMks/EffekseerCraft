package com.github.mrmks.mc.efscraft.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public interface NetworkPacket {

    interface Codec<T extends NetworkPacket> {
        default void read(T packet, InputStream stream) throws IOException {}
        default void write(T packet, OutputStream stream) throws IOException {}
    }

    interface ClientHandler<IN extends NetworkPacket, OUT extends NetworkPacket> {
        OUT handlePacket(IN packetIn);
    }

    interface ServerHandler<IN extends NetworkPacket, OUT extends NetworkPacket> {
        OUT handlePacket(IN packet, UUID sender);
    }
}
