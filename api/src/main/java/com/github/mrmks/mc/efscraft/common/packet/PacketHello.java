package com.github.mrmks.mc.efscraft.common.packet;

import com.github.mrmks.mc.efscraft.common.Constants;

import java.io.*;

public class PacketHello implements NetworkPacket {

    public int version;
    public PacketHello() {}

    static final NetworkPacket.Codec<PacketHello> CODEC = new NetworkPacket.Codec<PacketHello>() {
        @Override
        public void read(PacketHello packet, InputStream stream) throws IOException {
            packet.version = new DataInputStream(stream).readInt();
        }

        @Override
        public void write(PacketHello packet, OutputStream stream) throws IOException {
            new DataOutputStream(stream).writeInt(Constants.PROTOCOL_VERSION);
        }
    };
}
