package com.github.mrmks.mc.efscraft.client;

public interface EntityConvert {

    boolean isValid(int entityId);

    boolean isAlive(int entityId);

    double[] getPosition(int entityId);

    double[] getPrevPosition(int entityId);

    float[] getRotation(int entityId);

    float[] getPrevRotation(int entityId);

    boolean canUseHead(int entityId);

    float[] getHeadRotation(int entityId);

    float[] getPrevHeadRotation(int entityId);

    boolean canUseRender(int entityId);

    float[] getRenderRotation(int entityId);

    float[] getPrevRenderRotation(int entityId);
}
