package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketTrigger implements IMessage {

    private String effect, emitter;
    private int id;

    public SPacketTrigger() {}

    public SPacketTrigger(String effect, String emitter, int id) {
        this.effect = effect;
        this.emitter = emitter;
        this.id = id;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        effect = stream.readUTF();
        emitter = stream.readUTF();
        id = stream.readByte();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(effect);
        stream.writeUTF(emitter);
        stream.writeByte(id);
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
}
