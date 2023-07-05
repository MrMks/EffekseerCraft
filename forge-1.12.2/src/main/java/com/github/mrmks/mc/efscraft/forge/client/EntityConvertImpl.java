package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EntityConvert;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class EntityConvertImpl implements EntityConvert<Entity> {

    private static final Vec2f EMPTY_FLOAT_2 = new Vec2f();

    @Override
    public Entity findEntity(int entityId) {
        return Minecraft.getMinecraft().world.getEntityByID(entityId);
    }

    @Override
    public boolean isValid(Entity entity) {
        return entity.isAddedToWorld();
    }

    @Override
    public boolean isAlive(Entity entity) {
        return entity.isAddedToWorld() && entity.isEntityAlive();
    }

    @Override
    public Vec3f getPosition(Entity entity) {
        return new Vec3f(entity.posX, entity.posY, entity.posZ);
    }

    @Override
    public Vec3f getPrevPosition(Entity entity) {
        return new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }

    @Override
    public Vec2f getRotation(Entity entity) {
        return new Vec2f(entity.rotationYaw, entity.rotationPitch);
    }

    @Override
    public Vec2f getPrevRotation(Entity entity) {
        return new Vec2f(entity.prevRotationYaw, entity.prevRotationPitch);
    }

    private EntityLivingBase findEntityLivingBase(Entity entity) {
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    @Override
    public boolean canUseHead(Entity entity) {
        return findEntityLivingBase(entity) != null;
    }

    @Override
    public Vec2f getHeadRotation(Entity entity) {
        EntityLivingBase base = findEntityLivingBase(entity);
        return base == null ? EMPTY_FLOAT_2 : new Vec2f(base.rotationYawHead, base.rotationPitch);
    }

    @Override
    public Vec2f getPrevHeadRotation(Entity entity) {
        EntityLivingBase base = findEntityLivingBase(entity);
        return base == null ? EMPTY_FLOAT_2 : new Vec2f(base.prevRotationYawHead, base.prevRotationPitch);
    }

    @Override
    public boolean canUseRender(Entity entity) {
        return findEntityLivingBase(entity) != null;
    }

    @Override
    public Vec2f getRenderRotation(Entity entity) {
        EntityLivingBase base = findEntityLivingBase(entity);

        return base == null ? EMPTY_FLOAT_2 : new Vec2f(base.renderYawOffset, base.rotationPitch);
    }

    @Override
    public Vec2f getPrevRenderRotation(Entity entity) {
        EntityLivingBase base = findEntityLivingBase(entity);
        return base == null ? EMPTY_FLOAT_2 : new Vec2f(base.prevRenderYawOffset, base.rotationPitch);
    }
}
