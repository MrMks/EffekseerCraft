package com.github.mrmks.mc.efscraft.client;

public interface EntityConvert {

    boolean isValid(int entity);

    boolean isAlive(int entity);

    double[] getPosition(int entity);

    double[] getPrevPosition(int entity);

    float[] getRotation(int entity);

    float[] getPrevRotation(int entity);

    boolean canUseHead(int entity);

    float[] getHeadRotation(int entity);

    float[] getPrevHeadRotation(int entity);

    boolean canUseRender(int entity);

    float[] getRenderRotation(int entity);

    float[] getPrevRenderRotation(int entity);
}
