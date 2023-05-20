package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.FloatBuffer;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        Entity entity = Minecraft.getInstance().cameraEntity;
        if (entity == null)
            return null;
        else {
            Vector3d v3d = entity.position();
            return new double[] {v3d.x, v3d.y, v3d.z};
        }
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        Entity entity = Minecraft.getInstance().cameraEntity;
        if (entity == null)
            return null;
        else {
            return new double[] {entity.xOld, entity.yOld, entity.zOld};
        }
    }

    private Matrix4f camMat, projMat;

    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        camMat.store(buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        projMat.store(buffer);
    }

    @SubscribeEvent
    public void onRenderEvent(RenderWorldLastEvent event) {
        this.camMat = event.getMatrixStack().last().pose();
        this.projMat = event.getProjectionMatrix();

        updateAndRender(event.getPartialTicks(), event.getFinishTimeNano(), 1000_000_000, Minecraft.getInstance().isPaused());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
        if (world != null && world.isClientSide())
            unloadRender();
    }
}
