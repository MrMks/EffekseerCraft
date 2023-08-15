package com.github.mrmks.mc.efscraft.client.event;

import com.github.mrmks.mc.efscraft.client.IEfsClientEvent;
import com.github.mrmks.mc.efscraft.math.Matrix4f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

public class EfsRenderEvent implements IEfsClientEvent {

    public enum Phase { START, END }

    private final float partial;
    private final long nanoNow;
    private final boolean pausing;
    private final Matrix4f matProj;
    private final Matrix4f matModel;
    private final Phase phase;

    public EfsRenderEvent(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel, Phase phase) {
        this.partial = partial;
        this.nanoNow = nanoNow;
        this.pausing = pausing;
        this.matProj = matProj;
        this.matModel = matModel;
        this.phase = phase;
    }

    public EfsRenderEvent(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel, Vec3f vPos, Vec3f vPrevPos, Phase phase) {
        this(partial, nanoNow, pausing, matProj, matModel.copy().translatef(vPrevPos.copy().linearTo(vPos, partial).negative()), phase);
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

    public Phase getPhase() {
        return phase;
    }

}
