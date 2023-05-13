package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EntityConvert;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class EntityConvertImpl implements EntityConvert {

    private static final double[] EMPTY_DOUBLE_3 = new double[3];
    private static final float[] EMPTY_FLOAT_2 = new float[2];

    private Entity findEntity(int entityId) {
        Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
        return entity != null && entity.isAddedToWorld() && entity.isEntityAlive() ? entity : null;
    }

    @Override
    public boolean isValid(int entityId) {
        Entity entity = findEntity(entityId);
        return entity != null;
    }

    @Override
    public boolean isAlive(int entityId) {
        return isValid(entityId);
    }

    @Override
    public double[] getPosition(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_DOUBLE_3 : new double[] {entity.posX, entity.posY, entity.posZ};
    }

    @Override
    public double[] getPrevPosition(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_DOUBLE_3 : new double[] {entity.prevPosX, entity.prevPosY, entity.prevPosZ};
    }

    @Override
    public float[] getRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.rotationYaw, entity.rotationPitch};
    }

    @Override
    public float[] getPrevRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.prevRotationYaw, entity.prevRotationPitch};
    }

    private EntityLivingBase findEntityLivingBase(int entityId) {
        Entity entity = findEntity(entityId);

        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    @Override
    public boolean canUseHead(int entityId) {
        return findEntityLivingBase(entityId) != null;
    }

    @Override
    public float[] getHeadRotation(int entityId) {
        EntityLivingBase base = findEntityLivingBase(entityId);
        return base == null ? EMPTY_FLOAT_2 : new float[] {base.rotationYawHead, base.rotationPitch};
    }

    @Override
    public float[] getPrevHeadRotation(int entityId) {
        EntityLivingBase base = findEntityLivingBase(entityId);
        return base == null ? EMPTY_FLOAT_2 : new float[] {base.prevRotationYawHead, base.prevRotationPitch};
    }

    @Override
    public boolean canUseRender(int entityId) {
        return findEntityLivingBase(entityId) != null;
    }

    @Override
    public float[] getRenderRotation(int entityId) {
        EntityLivingBase base = findEntityLivingBase(entityId);

        return base == null ? EMPTY_FLOAT_2 : new float[] {base.renderYawOffset, base.rotationPitch};
    }

    @Override
    public float[] getPrevRenderRotation(int entityId) {
        EntityLivingBase base = findEntityLivingBase(entityId);
        return base == null ? EMPTY_FLOAT_2 : new float[] {base.prevRenderYawOffset, base.prevRotationPitch};
    }
}
