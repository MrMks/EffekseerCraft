package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SPacketPlayAt extends SPacketPlayAbstract {

    private float[] posModel;

    public SPacketPlayAt() {}

    public SPacketPlayAt(String effect, String emitter, int lifespan, float x, float y, float z) {
        super(effect, emitter, lifespan);
        this.posModel = new float[] {x, y, z};
    }

    public float[] getModelPos() {
        return posModel.clone();
    }

    public void read(DataInput stream) throws IOException {
        super.read(stream);
        posModel = new float[] {stream.readFloat(), stream.readFloat(), stream.readFloat()};
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        super.write(stream);

        stream.writeFloat(posModel[0]);
        stream.writeFloat(posModel[1]);
        stream.writeFloat(posModel[2]);
    }
}
