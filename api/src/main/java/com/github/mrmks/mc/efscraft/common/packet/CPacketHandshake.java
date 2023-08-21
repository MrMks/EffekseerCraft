package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CPacketHandshake implements NetworkPacket {

    private byte[] encrypted;

    public CPacketHandshake() {}

    public CPacketHandshake(byte[] encrypted) {
        this.encrypted = encrypted;
    }

    static final Codec<CPacketHandshake> CODEC = new Codec<CPacketHandshake>() {
        @Override
        public void read(CPacketHandshake packet, DataInput stream) throws IOException {
            byte[] bytes = new byte[stream.readInt()];
            stream.readFully(bytes);
            packet.encrypted = bytes;
        }

        @Override
        public void write(CPacketHandshake packet, DataOutput stream) throws IOException {
            stream.writeInt(packet.encrypted.length);
            stream.write(packet.encrypted);
        }
    };

}
