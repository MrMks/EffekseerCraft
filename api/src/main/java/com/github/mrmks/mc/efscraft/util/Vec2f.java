package com.github.mrmks.mc.efscraft.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Vec2f {
    float x, y;
    public Vec2f() {}
    public Vec2f(float x, float y) { set(x, y); }

    void set(float x, float y) { this.x = x; this.y = y; }
    float[] toArray() { return new float[] {x, y}; }
    public Vec2f read(DataInput input) throws IOException { set(input.readFloat(), input.readFloat()); return this; }
    public void write(DataOutput output) throws IOException { for (float f : toArray()) output.writeFloat(f); }

    public Vec2f add(Vec2f o) { return new Vec2f(x + o.x, y + o.y); }
    public Vec2f add(float x, float y) { return new Vec2f(this.x + x, this.y + y); }
}
