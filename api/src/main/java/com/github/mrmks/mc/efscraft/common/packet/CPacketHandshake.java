package com.github.mrmks.mc.efscraft.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CPacketHandshake implements NetworkPacket {

    private byte[] encrypted;

    public CPacketHandshake() {}

    public CPacketHandshake(byte[] encrypted) {
        this.encrypted = encrypted;
    }

    static final Codec<CPacketHandshake> CODEC = new Codec<CPacketHandshake>() {
        @Override
        public void read(CPacketHandshake packet, InputStream stream) throws IOException {
        }

        @Override
        public void write(CPacketHandshake packet, OutputStream stream) throws IOException {
        }
    };

}
