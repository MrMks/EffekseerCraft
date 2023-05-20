package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EntityConvert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;

class EntityConvertImpl implements EntityConvert {

    private static final double[] EMPTY_DOUBLE_3 = new double[3];
    private static final float[] EMPTY_FLOAT_2 = new float[2];

    private Entity findEntity(int entityId) {
        ClientWorld world = Minecraft.getInstance().level;
        Entity entity = world == null ? null : world.getEntity(entityId);
        return entity != null && entity.isAlive() && entity.isAddedToWorld() ? entity : null;
    }

    @Override
    public boolean isValid(int entityId) {
        return findEntity(entityId) != null;
    }

    @Override
    public boolean isAlive(int entityId) {
        return isValid(entityId);
    }

    @Override
    public double[] getPosition(int entityId) {
        Entity entity = findEntity(entityId);
        Vector3d v3d = entity == null ? null : entity.position();

        return v3d == null ? EMPTY_DOUBLE_3 : new double[] {v3d.x, v3d.y, v3d.z};
    }

    @Override
    public double[] getPrevPosition(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_DOUBLE_3 : new double[] {entity.xOld, entity.yOld, entity.zOld};
    }

    @Override
    public float[] getRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yRot, entity.xRot};
    }

    @Override
    public float[] getPrevRotation(int entityId) {
        Entity entity = findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yRotO, entity.xRotO};
    }

    @Override
    public boolean canUseHead(int entityId) {
        Entity entity = findEntity(entityId);

        return entity instanceof LivingEntity;
    }

    @Override
    public float[] getHeadRotation(int entityId) {
        LivingEntity entity = (LivingEntity) findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yHeadRot, entity.xRot};
    }

    @Override
    public float[] getPrevHeadRotation(int entityId) {
        LivingEntity entity = (LivingEntity) findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yHeadRotO, entity.xRotO};
    }

    @Override
    public boolean canUseRender(int entity) {
        return findEntity(entity) instanceof LivingEntity;
    }

    @Override
    public float[] getRenderRotation(int entityId) {
        LivingEntity entity = (LivingEntity) findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yBodyRot, entity.xRot};
    }

    @Override
    public float[] getPrevRenderRotation(int entityId) {
        LivingEntity entity = (LivingEntity) findEntity(entityId);

        return entity == null ? EMPTY_FLOAT_2 : new float[] {entity.yBodyRotO, entity.xRotO};
    }
}
