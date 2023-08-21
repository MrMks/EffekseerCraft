package com.github.mrmks.mc.efscraft.common.packet;

import java.io.*;

public class SPacketTrigger implements NetworkPacket {

    private String effect, emitter;
    private int id;

    public SPacketTrigger() {}

    public SPacketTrigger(String effect, String emitter, int id) {
        this.effect = effect;
        this.emitter = emitter;
        this.id = id;
    }

    public String getKey() {
        return effect;
    }

    public String getEmitter() {
        return emitter;
    }

    public int getTrigger() {
        return id;
    }

    static final NetworkPacket.Codec<SPacketTrigger> CODEC = new NetworkPacket.Codec<SPacketTrigger>() {
        @Override
        public void read(SPacketTrigger packet, InputStream inputStream) throws IOException {
            DataInputStream stream = new DataInputStream(inputStream);
            packet.effect = stream.readUTF();
            packet.emitter = stream.readUTF();
            packet.id = stream.readByte();
        }

        @Override
        public void write(SPacketTrigger packet, OutputStream outputStream) throws IOException {
            DataOutputStream stream = new DataOutputStream(outputStream);
            stream.writeUTF(packet.effect);
            stream.writeUTF(packet.emitter);
            stream.writeByte(packet.id);
        }
    };

}
