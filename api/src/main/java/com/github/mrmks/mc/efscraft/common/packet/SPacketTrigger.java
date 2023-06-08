package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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

    enum Codec implements NetworkPacket.Codec<SPacketTrigger> {
        INSTANCE;

        @Override
        public void read(SPacketTrigger packet, DataInput stream) throws IOException {
            packet.effect = stream.readUTF();
            packet.emitter = stream.readUTF();
            packet.id = stream.readByte();
        }

        @Override
        public void write(SPacketTrigger packet, DataOutput stream) throws IOException {
            stream.writeUTF(packet.effect);
            stream.writeUTF(packet.emitter);
            stream.writeByte(packet.id);
        }
    }
}
