package com.github.mrmks.mc.efscraft.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Vec3f {
    float x, y, z;
    public Vec3f() {}
    public Vec3f(float x, float y, float z) { set(x, y, z); }
    public void set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public float[] toArray() { return new float[] {x, y, z}; }
    public Vec3f read(DataInput input) throws IOException { set(input.readFloat(), input.readFloat(), input.readFloat()); return this; }
    public void write(DataOutput output) throws IOException { for (float f : toArray()) output.writeFloat(f); }
}
