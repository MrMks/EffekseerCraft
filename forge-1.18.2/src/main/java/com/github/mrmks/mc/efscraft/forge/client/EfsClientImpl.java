package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.client.IEfsClientAdaptor;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.forge.common.NetworkWrapper;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class EfsClientImpl extends EfsClient<Entity, LocalPlayer, ByteBufInputStream, ByteBufOutputStream> {

    public EfsClientImpl(NetworkWrapper wrapper, RendererImpl renderer, LogAdaptor logger, boolean autoReply, File folder) {
        super(new AdaptorImpl(wrapper, renderer), logger, autoReply, folder);
        wrapper.setClient(this);
    }

    static class AdaptorImpl implements IEfsClientAdaptor<Entity, LocalPlayer, ByteBufInputStream, ByteBufOutputStream> {

        final NetworkWrapper wrapper;
        final RendererImpl renderer;
        AdaptorImpl(NetworkWrapper wrapper, RendererImpl renderer) {
            this.wrapper = wrapper;
            this.renderer = renderer;
        }

        @Override
        public Entity findEntity(int entityId) {
            Level level = Minecraft.getInstance().level;
            return level == null ? null : level.getEntity(entityId);
        }

        @Override
        public LocalPlayer getPlayerEntity(Entity entity) {
            return entity instanceof LocalPlayer ? (LocalPlayer) entity : null;
        }

        @Override
        public boolean isAlive(Entity entity) {
            return entity.isAlive() && entity.isAddedToWorld();
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
            return new Vec2f(entity.getYRot(), entity.getXRot());
        }

        @Override
        public Vec2f getEntityPrevAngle(Entity entity) {
            return new Vec2f(entity.yRotO, entity.xRotO);
        }

        @Override
        public boolean canUseHead(Entity entity) {
            return entity instanceof LivingEntity;
        }

        @Override
        public Vec2f getEntityHeadAngle(Entity entity) {
            LivingEntity living = (LivingEntity) entity;
            return new Vec2f(living.yHeadRot, entity.getXRot());
        }

        @Override
        public Vec2f getEntityHeadPrevAngle(Entity entity) {
            LivingEntity living = (LivingEntity) entity;
            return new Vec2f(living.yHeadRotO, entity.xRotO);
        }

        @Override
        public boolean canUseBody(Entity entity) {
            return entity instanceof LivingEntity;
        }

        @Override
        public Vec2f getEntityBodyAngle(Entity entity) {
            LivingEntity living = (LivingEntity) entity;
            return new Vec2f(living.yBodyRot, entity.getXRot());
        }

        @Override
        public Vec2f getEntityBodyPrevAngle(Entity entity) {
            LivingEntity living = (LivingEntity) entity;
            return new Vec2f(living.yBodyRotO, entity.xRotO);
        }

        @Override
        public InputStream loadResource(String namespace, String key, String path) throws IOException {
            String full = String.format("effects/%s/%s", key, path);
            ResourceLocation location;
            try {
                location = new ResourceLocation(namespace, full);
            } catch (ResourceLocationException e) {
                throw new FileNotFoundException(full);
            }
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(location);

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
            wrapper.send(dataOutput.buffer());
        }

        @Override
        public void drawEffect(EfsRenderEvent event, Runnable drawer) {
            renderer.drawEffect(event.getPhase(), drawer);
        }

        @Override
        public void schedule(Runnable runnable) {
            Minecraft.getInstance().submit(runnable);
        }
    }
}
