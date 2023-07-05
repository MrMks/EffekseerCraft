package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EntityConvert;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("SuspiciousNameCombination")
class EntityConvertImpl implements EntityConvert<Entity> {

    private static final Vec3f EMPTY_FLOAT_3 = new Vec3f();
    private static final Vec2f EMPTY_FLOAT_2 = new Vec2f();

    @Override
    public Entity findEntity(int entityId) {
        ClientLevel world = Minecraft.getInstance().level;
        return world == null ? null : world.getEntity(entityId);
    }

    @Override
    public boolean isValid(Entity entity) {
        return entity.isAddedToWorld();
    }

    @Override
    public boolean isAlive(Entity entity) {
        return entity.isAddedToWorld() && entity.isAlive();
    }

    @Override
    public Vec3f getPosition(Entity entity) {
        Vec3 v3d = entity == null ? null : entity.position();

        return v3d == null ? EMPTY_FLOAT_3 : new Vec3f(v3d.x, v3d.y, v3d.z);
    }

    @Override
    public Vec3f getPrevPosition(Entity entity) {
        return new Vec3f(entity.xOld, entity.yOld, entity.zOld);
    }

    @Override
    public Vec2f getRotation(Entity entity) {
        return new Vec2f(entity.getYRot(), entity.getXRot());
    }

    @Override
    public Vec2f getPrevRotation(Entity entity) {
        return new Vec2f(entity.yRotO, entity.xRotO);
    }

    private LivingEntity castLivingEntity(Entity entity) {
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    @Override
    public boolean canUseHead(Entity entity) {
        return castLivingEntity(entity) != null;
    }

    @Override
    public Vec2f getHeadRotation(Entity bast) {
        LivingEntity entity = castLivingEntity(bast);

        return entity == null ? EMPTY_FLOAT_2 : new Vec2f(entity.yHeadRot, entity.getXRot());
    }

    @Override
    public Vec2f getPrevHeadRotation(Entity base) {
        LivingEntity entity = castLivingEntity(base);

        return entity == null ? EMPTY_FLOAT_2 : new Vec2f(entity.yHeadRotO, entity.xRotO);
    }

    @Override
    public boolean canUseRender(Entity entity) {
        return castLivingEntity(entity) != null;
    }

    @Override
    public Vec2f getRenderRotation(Entity base) {
        LivingEntity entity = castLivingEntity(base);

        return entity == null ? EMPTY_FLOAT_2 : new Vec2f(entity.yBodyRot, entity.getXRot());
    }

    @Override
    public Vec2f getPrevRenderRotation(Entity base) {
        LivingEntity entity = castLivingEntity(base);

        return entity == null ? EMPTY_FLOAT_2 : new Vec2f(entity.yBodyRotO, entity.xRotO);
    }
}
