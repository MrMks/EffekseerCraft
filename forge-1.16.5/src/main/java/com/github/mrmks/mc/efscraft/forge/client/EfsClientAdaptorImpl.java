package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.IEfsClientAdaptor;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.forge.common.NetworkWrapper;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("SuspiciousNameCombination")
class EfsClientAdaptorImpl implements IEfsClientAdaptor<Entity, ClientPlayerEntity, ByteBufInputStream, ByteBufOutputStream> {

    NetworkWrapper wrapper;
    RendererImpl renderer;

    EfsClientAdaptorImpl(NetworkWrapper wrapper, RendererImpl renderer) {
        this.wrapper = wrapper;
        this.renderer = renderer;
    }

    @Override
    public Entity findEntity(int entityId) {
        ClientWorld world = Minecraft.getInstance().level;
        return world == null ? null : world.getEntity(entityId);
    }

    @Override
    public ClientPlayerEntity getPlayerEntity(Entity entity) {
        return entity instanceof ClientPlayerEntity ? (ClientPlayerEntity) entity : null;
    }

    @Override
    public boolean isAlive(Entity entity) {
        return entity.isAddedToWorld() && entity.isAlive();
    }

    @Override
    public Vec3f getEntityPos(Entity entity) {
        return new Vec3f(entity.getX(), entity.getY(), entity.getZ());
    }

    @Override
    public Vec3f getEntityPrevPos(Entity entity) {
        return new Vec3f(entity.xOld, entity.yOld, entity.zOld);
    }

    @Override
    public Vec2f getEntityAngle(Entity entity) {
        return new Vec2f(entity.yRot, entity.xRot);
    }

    @Override
    public Vec2f getEntityPrevAngle(Entity entity) {
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
    public Vec2f getEntityHeadAngle(Entity entity) {
        LivingEntity living = castLivingEntity(entity);
        return new Vec2f(living.yHeadRot, entity.xRot);
    }

    @Override
    public Vec2f getEntityHeadPrevAngle(Entity entity) {
        LivingEntity living = castLivingEntity(entity);
        return new Vec2f(living.yHeadRotO, entity.xRotO);
    }

    @Override
    public boolean canUseBody(Entity entity) {
        return castLivingEntity(entity) != null;
    }

    @Override
    public Vec2f getEntityBodyAngle(Entity entity) {
        LivingEntity living = castLivingEntity(entity);
        return new Vec2f(living.yBodyRot, entity.xRot);
    }

    @Override
    public Vec2f getEntityBodyPrevAngle(Entity entity) {
        return new Vec2f(castLivingEntity(entity).yBodyRotO, entity.xRotO);
    }

    @Override
    public InputStream loadResource(String namespace, String key, String path) throws IOException {
        ResourceLocation location;
        String full = String.format("effects/%s/%s", key, path);
        try {
            location = new ResourceLocation(namespace, full);
        } catch (ResourceLocationException e) {
            throw new FileNotFoundException(full);
        }
        IResource resource = Minecraft.getInstance().getResourceManager().getResource(location);
        return resource.getInputStream();
    }

    @Override
    public ByteBufOutputStream createOutput() {
        return new ByteBufOutputStream(Unpooled.buffer());
    }

    @Override
    public void closeOutput(ByteBufOutputStream output) throws IOException {
        ByteBuf buf = output.buffer();
        if (buf.refCnt() > 0)
            buf.release();
    }

    @Override
    public void sendPacket(ByteBufOutputStream dataOutput) {
        wrapper.sendTo(dataOutput.buffer());
    }

    @Override
    public void drawEffect(EfsRenderEvent event, Runnable drawer) {
        renderer.drawEffect(event, drawer);
    }

    @Override
    public void schedule(Runnable runnable) {
        Minecraft.getInstance().submit(runnable);
    }

}
