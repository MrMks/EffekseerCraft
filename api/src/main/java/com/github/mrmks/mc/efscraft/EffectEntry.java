package com.github.mrmks.mc.efscraft;

abstract class EffectEntry {
    protected String effect;
    protected float[] localPos, localRot, modelPos, modelRot, scale, dynamic;
    protected boolean followX, followY, followZ, followYaw, followPitch;
    protected boolean inheritYaw, inheritPitch;
    protected boolean useHead, useRender;
    protected boolean overwrite;
    protected int skipFrames, lifespan;
}
