package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketStop implements IMessage {
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

    @Override
    public void read(DataInput stream) throws IOException {
        this.key = stream.readUTF();
        this.emitter = stream.readUTF();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(key);
        stream.writeUTF(emitter);
    }
}
