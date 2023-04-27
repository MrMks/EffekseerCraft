package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketStop implements IMessage {
    private String effect, emitter; // effect and emitter to search which emitter

    public SPacketStop() {}

    public SPacketStop(String effect, String emitter) {
        this.effect = effect;
        this.emitter = emitter;
    }

    public String getEffect() {
        return effect;
    }

    public String getEmitter() {
        return emitter;
    }

    @Override
    public void read(DataInput stream) throws IOException {
        this.effect = stream.readUTF();
        this.emitter = stream.readUTF();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(effect);
        stream.writeUTF(emitter);
    }
}
