package com.github.mrmks.mc.efscraft.packet;

import com.github.mrmks.mc.efscraft.Constants;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PacketHello implements IMessage {

    private int version;
    public PacketHello() {}

    @Override
    public void read(DataInput stream) throws IOException {
        version = stream.readInt();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeInt(Constants.PROTOCOL_VERSION);
    }

    public int getVersion() {
        return version;
    }
}
