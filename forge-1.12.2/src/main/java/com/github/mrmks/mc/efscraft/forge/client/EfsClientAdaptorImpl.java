package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.IEfsClientAdaptor;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.forge.common.NetworkWrapper;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public class EfsClientAdaptorImpl implements IEfsClientAdaptor<Entity, EntityPlayerSP, ByteBufInputStream, ByteBufOutputStream> {

    private final NetworkWrapper wrapper;
    private final RendererImpl renderer;

    EfsClientAdaptorImpl(NetworkWrapper wrapper, RendererImpl renderer) {
        this.wrapper = wrapper;
        this.renderer = renderer;
    }

    @Override
    public Entity findEntity(int entityId) {
        WorldClient world = Minecraft.getMinecraft().world;
        return world == null ? null : world.getEntityByID(entityId);
    }

    @Override
    public EntityPlayerSP getPlayerEntity(Entity entity) {
        return entity instanceof EntityPlayerSP ? (EntityPlayerSP) entity : null;
    }

    @Override
    public boolean isAlive(Entity entity) {
        return entity.isAddedToWorld() && entity.isEntityAlive();
    }

    @Override
    public Vec3f getEntityPos(Entity entity) {
        return new Vec3f(entity.posX, entity.posY, entity.posZ);
    }

    @Override
    public Vec3f getEntityPrevPos(Entity entity) {
        return new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }

    @Override
    public Vec2f getEntityAngle(Entity entity) {
        return new Vec2f(entity.rotationYaw, entity.rotationPitch);
    }

    @Override
    public Vec2f getEntityPrevAngle(Entity entity) {
        return new Vec2f(entity.prevRotationYaw, entity.prevRotationPitch);
    }

    @Override
    public boolean canUseHead(Entity entity) {
        return entity instanceof EntityLivingBase;
    }

    @Override
    public Vec2f getEntityHeadAngle(Entity entity) {
        EntityLivingBase living = (EntityLivingBase) entity;
        return new Vec2f(living.rotationYawHead, living.rotationPitch);
    }

    @Override
    public Vec2f getEntityHeadPrevAngle(Entity entity) {
        EntityLivingBase living = (EntityLivingBase) entity;
        return new Vec2f(living.prevRotationYawHead, living.prevRotationPitch);
    }

    @Override
    public boolean canUseBody(Entity entity) {
        return entity instanceof EntityLivingBase;
    }

    @Override
    public Vec2f getEntityBodyAngle(Entity entity) {
        EntityLivingBase living = (EntityLivingBase) entity;
        return new Vec2f(living.renderYawOffset, living.rotationPitch);
    }

    @Override
    public Vec2f getEntityBodyPrevAngle(Entity entity) {
        EntityLivingBase living = (EntityLivingBase) entity;
        return new Vec2f(living.prevRenderYawOffset, living.prevRotationPitch);
    }

    @Override
    public InputStream loadResource(String namespace, String key, String path) throws IOException {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        IResource resource = manager.getResource(new ResourceLocation(namespace, String.format("effects/%s/%s", key, path)));
        return resource.getInputStream();
    }

    @Override
    public ByteBufOutputStream createOutput() {
        return new ByteBufOutputStream(Unpooled.buffer());
    }

    @Override
    public void closeOutput(ByteBufOutputStream output) throws IOException {
        output.buffer().release();
    }

    @Override
    public void sendPacket(ByteBufOutputStream dataOutput) {
        wrapper.sendTo(dataOutput.buffer());
    }

    @Override
    public void drawEffect(EfsRenderEvent event, Runnable drawer) {
        renderer.renderWorld(event, drawer);
    }

    @Override
    public void schedule(Runnable runnable) {
        Minecraft.getMinecraft().addScheduledTask(runnable);
    }
}
