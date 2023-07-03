package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EntityConvert;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class EntityConvertImpl implements EntityConvert {

    private static final double[] DOUBLE_3 = new double[3];
    private static final float[] FLOAT_2 = new float[2];

    private Entity findEntity(int id) {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(id);
    }

    @Override
    public boolean isValid(int entityId) {
        Entity entity = findEntity(entityId);

        return entity != null && entity.isAddedToWorld();
    }

    @Override
    public boolean isAlive(int entityId) {
        Entity entity = findEntity(entityId);

        return entity != null && entity.isAddedToWorld() && entity.isAlive();
    }

    @Override
    public double[] getPosition(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? DOUBLE_3 : new double[] {entity.getX(), entity.getY(), entity.getZ()};
    }

    @Override
    public double[] getPrevPosition(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? DOUBLE_3 : new double[] {entity.xOld, entity.yOld, entity.zOld};
    }

    @Override
    public float[] getRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? FLOAT_2 : new float[] { entity.getYRot(), entity.getXRot() };
    }

    @Override
    public float[] getPrevRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? FLOAT_2 : new float[] { entity.yRotO, entity.xRotO };
    }

    @Override
    public boolean canUseHead(int entityId) {
        return findEntity(entityId) instanceof LivingEntity;
    }

    @Override
    public float[] getHeadRotation(int entityId) {
        Entity entity = findEntity(entityId);
        LivingEntity living = entity instanceof LivingEntity ? (LivingEntity) entity : null;

        return living == null ? FLOAT_2 : new float[] { living.yHeadRot, living.getXRot() };
    }

    @Override
    public float[] getPrevHeadRotation(int entityId) {
        Entity entity = findEntity(entityId);
        LivingEntity living = entity instanceof LivingEntity ? (LivingEntity) entity : null;

        return living == null ? FLOAT_2 : new float[] { living.yHeadRotO, living.getXRot() };
    }

    @Override
    public boolean canUseRender(int entityId) {
        return findEntity(entityId) instanceof LivingEntity;
    }

    @Override
    public float[] getRenderRotation(int entityId) {
        Entity entity = findEntity(entityId);
        LivingEntity living = entity instanceof LivingEntity ? (LivingEntity) entity : null;

        return living == null ? FLOAT_2 : new float[] { living.yBodyRot, living.getXRot() };
    }

    @Override
    public float[] getPrevRenderRotation(int entityId) {
        Entity entity = findEntity(entityId);
        LivingEntity living = entity instanceof LivingEntity ? (LivingEntity) entity : null;

        return living == null ? FLOAT_2 : new float[] { living.yBodyRotO, living.getXRot() };
    }
}
