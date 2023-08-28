package com.github.mrmks.mc.efscraft.server;

abstract class ServerRegistry {

    static class FollowArguments {

        protected boolean followX, followY, followZ, followYaw, followPitch;
        protected boolean baseOnCurrentYaw, baseOnCurrentPitch;
        protected boolean directionFromHead, directionFromBody;

        protected FollowArguments() {
            this.followX = this.followY = this.followZ = this.followYaw = this.followPitch = false;
            this.baseOnCurrentYaw = this.baseOnCurrentPitch = true;
            this.directionFromHead = this.directionFromBody = false;
        }

        private FollowArguments(FollowArguments other) {
            this.followX = other.followX;
            this.followY = other.followY;
            this.followZ = other.followZ;
            this.followYaw = other.followYaw;
            this.followPitch = other.followPitch;

            this.baseOnCurrentYaw = other.baseOnCurrentYaw;
            this.baseOnCurrentPitch = other.baseOnCurrentPitch;

            this.directionFromHead = other.directionFromHead;
            this.directionFromBody = other.directionFromBody;
        }
    }

    protected String effect;
    protected int lifespan, skipFrames;
    protected boolean overwrite;
    protected float[] localPos, localRot, modelPos, modelRot, scale, dynamic;
    protected FollowArguments followArgs;

    protected ServerRegistry() {
        this.effect = null;
        this.lifespan = -1;
        this.skipFrames = 0;
        this.overwrite = false;

        this.localPos = new float[3]; this.modelPos = new float[3];
        this.localRot = new float[2]; this.modelRot = new float[2];
        this.scale = new float[] {1, 1, 1};
        this.dynamic = null;

        this.followArgs = new FollowArguments();

    }

    protected ServerRegistry(String effect, int lifespan) {
        this();
        this.effect = effect;
        this.lifespan = lifespan;
    }

    protected ServerRegistry(ServerRegistry other) {
        this.effect = other.effect;

        this.localPos = other.localPos.clone();
        this.localRot = other.localRot.clone();
        this.modelPos = other.modelPos.clone();
        this.modelRot = other.modelRot.clone();
        this.scale = other.scale.clone();
        this.dynamic = other.dynamic == null ? null : other.dynamic.clone();

        this.followArgs = new FollowArguments(other.followArgs);

        this.overwrite = other.overwrite;

        this.skipFrames = other.skipFrames;
        this.lifespan = other.lifespan;
    }
}
