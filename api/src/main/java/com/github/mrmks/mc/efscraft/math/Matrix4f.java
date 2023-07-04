package com.github.mrmks.mc.efscraft.math;

public class Matrix4f {

    private static final float PI = (float) Math.PI;
    private static float sin(float rad) {
        return (float) Math.sin(rad);
    }

    private static float cos(float rad) {
        return (float) Math.cos(rad);
    }

    private float m00, m01, m02, m03;
    private float m10, m11, m12, m13;
    private float m20, m21, m22, m23;
    private float m30, m31, m32, m33;

    public Matrix4f() {}

    public Matrix4f(float[] floats) {

        if (floats == null || floats.length < 16) throw new IllegalArgumentException();

        m00 = floats[0]; m10 = floats[1]; m20 = floats[2]; m30 = floats[3];
        m01 = floats[4]; m11 = floats[5]; m21 = floats[6]; m31 = floats[7];
        m02 = floats[8]; m12 = floats[9]; m22 = floats[10]; m32 = floats[11];
        m03 = floats[12]; m13 = floats[13]; m23 = floats[14]; m33 = floats[15];
    }

    public Matrix4f(Matrix4f other) {
        m00 = other.m00; m01 = other.m01; m02 = other.m02; m03 = other.m03;
        m10 = other.m10; m11 = other.m11; m12 = other.m12; m13 = other.m13;
        m20 = other.m20; m21 = other.m21; m22 = other.m22; m23 = other.m23;
        m30 = other.m30; m31 = other.m31; m32 = other.m32; m33 = other.m33;
    }

    public Matrix4f identity() {

        m00 = m11 = m22 = m33 = 1;

        m01 = m02 = m03 = 0;
        m10 = m12 = m13 = 0;
        m20 = m21 = m23 = 0;
        m30 = m31 = m32 = 0;
        return this;
    }

    public Matrix4f translatef(float x, float y, float z) {
        m03 += m00 * x + m01 * y + m02 * z;
        m13 += m10 * x + m11 * y + m12 * z;
        m23 += m20 * x + m21 * y + m22 * z;
        return this;
    }

    public Matrix4f translatef(Vec3f vec) {
        return translatef(vec.x, vec.y, vec.z);
    }

    public Matrix4f translated(double x, double y, double z) {
        translatef((float) x, (float) y, (float) z);
        return this;
    }

    public Matrix4f rotateMC(float yaw, float pitch) {
        return rotate(-yaw, -pitch);
    }

    public Matrix4f rotateMC(Vec2f vec) {
        return rotateMC(vec.x, vec.y);
    }

    public Matrix4f rotate(float yaw, float pitch) {
        yaw *= PI / 180;
        pitch *= PI / 180;

        Matrix4f other = new Matrix4f();

        other.m02 = sin(yaw);
        other.m22 = cos(yaw);
        other.m10 = sin(pitch);
        other.m11 = cos(pitch);

        other.m00 =  other.m11 * other.m22;
        other.m01 = -other.m10 * other.m22;
        other.m20 = -other.m02 * other.m11;
        other.m21 =  other.m02 * other.m10;

        return mul33(other);
    }

    public Matrix4f rotate(Vec2f vec) {
        return rotate(vec.x, vec.y);
    }

    public Matrix4f scale(float x, float y, float z) {
        m00 *= x; m01 *= x; m02 *= x;
        m10 *= y; m11 *= y; m12 *= y;
        m20 *= z; m21 *= z; m22 *= z;
        return this;
    }

    public Matrix4f scale(Vec3f vec) {
        return scale(vec.x, vec.y, vec.z);
    }

    private Matrix4f mul33(Matrix4f other) {

        float t0, t1, t2;

        t0 = m00; t1 = m01; t2 = m02;
        m00 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20;
        m01 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21;
        m02 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22;

        t0 = m10; t1 = m11; t2 = m12;
        m10 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20;
        m11 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21;
        m12 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22;

        t0 = m20; t1 = m21; t2 = m22;
        m20 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20;
        m21 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21;
        m22 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22;

        return this;
    }

    public Matrix4f mul(Matrix4f other) {

        float t0, t1, t2, t3;

        t0 = m00; t1 = m01; t2 = m02; t3 = m03;
        m00 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20 + t3 * other.m30;
        m01 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21 + t3 * other.m31;
        m02 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22 + t3 * other.m32;
        m03 = t0 * other.m03 + t1 * other.m13 + t2 * other.m23 + t3 * other.m33;

        t0 = m10; t1 = m11; t2 = m12; t3 = m13;
        m10 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20 + t3 * other.m30;
        m11 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21 + t3 * other.m31;
        m12 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22 + t3 * other.m32;
        m13 = t0 * other.m03 + t1 * other.m13 + t2 * other.m23 + t3 * other.m33;

        t0 = m20; t1 = m21; t2 = m22; t3 = m23;
        m20 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20 + t3 * other.m30;
        m21 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21 + t3 * other.m31;
        m22 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22 + t3 * other.m32;
        m23 = t0 * other.m03 + t1 * other.m13 + t2 * other.m23 + t3 * other.m33;

        t0 = m30; t1 = m31; t2 = m32; t3 = m33;
        m30 = t0 * other.m00 + t1 * other.m10 + t2 * other.m20 + t3 * other.m30;
        m31 = t0 * other.m01 + t1 * other.m11 + t2 * other.m21 + t3 * other.m31;
        m32 = t0 * other.m02 + t1 * other.m12 + t2 * other.m22 + t3 * other.m32;
        m33 = t0 * other.m03 + t1 * other.m13 + t2 * other.m23 + t3 * other.m33;

        return this;
    }

    public float[] getFloats() {
        return new float[] {
                m00, m10, m20, m30,
                m01, m11, m21, m31,
                m02, m12, m22, m32,
                m03, m13, m23, m33
        };
    }

    @Override
    public String toString() {
        return String.format("%.2f\t%.2f\t%.2f\t%.2f\t\n%.2f\t%.2f\t%.2f\t%.2f\t\n%.2f\t%.2f\t%.2f\t%.2f\t\n%.2f\t%.2f\t%.2f\t%.2f\t\n",
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33
        );
    }

    public Matrix4f copy() {
        return new Matrix4f(this);
    }
}
