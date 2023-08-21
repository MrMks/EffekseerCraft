package com.github.mrmks.mc.efscraft.common.packet;

import java.io.*;

public class SPacketStop implements NetworkPacket {
    private String key, emitter; // effect and emitter to search which emitter

    public SPacketStop() {}

    public SPacketStop(String key, String emitter) {
        this.key = key;
        this.emitter = emitter;
    }

    public String getKey() {
        return key;
    }

    public String getEmitter() {
        return emitter;
    }

    static final NetworkPacket.Codec<SPacketStop> CODEC = new Codec<SPacketStop>() {
        @Override
        public void read(SPacketStop packet, InputStream inputStream) throws IOException {
            DataInputStream stream = new DataInputStream(inputStream);
            packet.key = stream.readUTF();
            packet.emitter = stream.readUTF();
        }

        @Override
        public void write(SPacketStop packet, OutputStream outputStream) throws IOException {
            DataOutputStream stream = new DataOutputStream(outputStream);
            stream.writeUTF(packet.key);
            stream.writeUTF(packet.emitter);
        }
    };
}
