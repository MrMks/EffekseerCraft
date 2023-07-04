package com.github.mrmks.mc.efscraft.math;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Vec3f {
    float x, y, z;
    public Vec3f() {}
    public Vec3f(Vec3f o) { set(o.x, o.y, o.z); }
    public Vec3f(float x, float y, float z) { set(x, y, z); }
    public Vec3f(double x, double y, double z) { set((float) x, (float) y, (float) z); }

    void set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    float[] toArray() { return new float[] {x, y, z}; }
    public Vec3f read(DataInput input) throws IOException { set(input.readFloat(), input.readFloat(), input.readFloat()); return this; }
    public void write(DataOutput output) throws IOException { for (float f : toArray()) output.writeFloat(f); }
    public float[] store() { return toArray(); }
    public Vec3f copy() { return new Vec3f(x, y, z); }

    public Vec3f add(Vec3f o) { set(x + o.x, y + o.y, z + o.z); return this; }
    public Vec3f add(float x, float y, float z) { set(this.x + x, this.y + y, this.z + z); return this; }
    public Vec3f negative() { set(-x, -y, -z); return this; }

    public Vec3f linearTo(Vec3f o, float f) {
        set(
                x + (o.x - x) * f,
                y + (o.y - y) * f,
                z + (o.z - z) * f
        );
        return this;
    }
}
