package com.github.mrmks.mc.efscraft.client.event;

import com.github.mrmks.mc.efscraft.client.IEfsClientEvent;
import com.github.mrmks.mc.efscraft.math.Matrix4f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

public abstract class EfsRenderEvent implements IEfsClientEvent {

    private final float partial;
    private final long nanoNow;
    private final boolean pausing;
    private final Matrix4f matProj;
    private final Matrix4f matModel;

    protected EfsRenderEvent(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel) {
        this.partial = partial;
        this.nanoNow = nanoNow;
        this.pausing = pausing;
        this.matProj = matProj;
        this.matModel = matModel;
    }

    protected EfsRenderEvent(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel, Vec3f vPos, Vec3f vPrevPos) {
        this(partial, nanoNow, pausing, matProj, matModel.copy().translatef(vPrevPos.copy().linearTo(vPos, partial).negative()));
    }

    public float getPartial() {
        return partial;
    }

    public long getNanoNow() {
        return nanoNow;
    }

    public Matrix4f getMatProj() {
        return matProj;
    }

    public Matrix4f getMatModel() {
        return matModel;
    }

    public boolean isGamePause() {
        return pausing;
    }

    public static class Prev extends EfsRenderEvent {
        public Prev(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel) {
            super(partial, nanoNow, pausing, matProj, matModel);
        }

        public Prev(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel, Vec3f vPos, Vec3f vPrevPos) {
            super(partial, nanoNow, pausing, matProj, matModel, vPos, vPrevPos);
        }
    }

    public static class Post extends EfsRenderEvent {
        public Post(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel) {
            super(partial, nanoNow, pausing, matProj, matModel);
        }

        public Post(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel, Vec3f vPos, Vec3f vPrevPos) {
            super(partial, nanoNow, pausing, matProj, matModel, vPos, vPrevPos);
        }
    }

}
