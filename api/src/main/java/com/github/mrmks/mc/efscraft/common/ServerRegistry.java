package com.github.mrmks.mc.efscraft.common;

abstract class ServerRegistry {
    protected String effect;
    protected int lifespan, skipFrames;
    protected boolean overwrite;
    protected float[] localPos, localRot, modelPos, modelRot, scale, dynamic;
    protected boolean followX, followY, followZ, followYaw, followPitch;
    protected boolean inheritYaw, inheritPitch;
    protected boolean useHead, useRender;

    protected ServerRegistry() {
        this.effect = null;
        this.lifespan = -1;
        this.skipFrames = 0;
        this.overwrite = false;

        this.localPos = new float[3]; this.modelPos = new float[3];
        this.localRot = new float[2]; this.modelRot = new float[2];
        this.scale = new float[] {1, 1, 1};
        this.dynamic = null;

        this.followX = this.followY = this.followZ = this.followYaw = this.followPitch = false;
        this.inheritYaw = this.inheritPitch = true;
        this.useHead = this.useRender = false;
    }

    protected ServerRegistry(String effect, int lifespan) {
        this();
        this.effect = effect;
        this.lifespan = lifespan;
    }

    protected ServerRegistry(ServerRegistry entry) {
        this.effect = entry.effect;

        this.localPos = entry.localPos.clone();
        this.localRot = entry.localRot.clone();
        this.modelPos = entry.modelPos.clone();
        this.modelRot = entry.modelRot.clone();
        this.scale = entry.scale.clone();
        this.dynamic = entry.dynamic == null ? null : entry.dynamic.clone();

        this.followX = entry.followX;
        this.followY = entry.followY;
        this.followZ = entry.followZ;
        this.followYaw = entry.followYaw;
        this.followPitch = entry.followPitch;

        this.inheritYaw = entry.inheritYaw;
        this.inheritPitch = entry.inheritPitch;

        this.useHead = entry.useHead;
        this.useRender = entry.useRender;

        this.overwrite = entry.overwrite;

        this.skipFrames = entry.skipFrames;
        this.lifespan = entry.lifespan;
    }
}
