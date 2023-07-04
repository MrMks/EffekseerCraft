package com.github.mrmks.mc.efscraft.math;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Vec2f {
    float x, y;
    public Vec2f() {}
    public Vec2f(Vec2f o) { set(o.x, o.y); }
    public Vec2f(float x, float y) { set(x, y); }
    public Vec2f(double x, double y) { set((float) x, (float) y); }

    void set(float x, float y) { this.x = x; this.y = y; }
    float[] toArray() { return new float[] {x, y}; }
    public Vec2f read(DataInput input) throws IOException { set(input.readFloat(), input.readFloat()); return this; }
    public void write(DataOutput output) throws IOException { for (float f : toArray()) output.writeFloat(f); }
    public float[] store() { return toArray(); }
    public Vec2f copy() { return new Vec2f(x, y); }

    public Vec2f add(Vec2f o) { set(x + o.x, y + o.y); return this; }
    public Vec2f add(float x, float y) { set(this.x + x, this.y + y); return this; }
    public Vec2f negative() { set(-x, -y); return this; }

    public Vec2f linearTo(Vec2f o, float partial) {
        set(
                x + (o.x - x) * partial,
                y + (o.y - y) * partial
        );
        return this;
    }
}
