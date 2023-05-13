package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

class RendererImpl extends Renderer {

    RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.posX, entity.posY, entity.posZ};
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.prevPosX, entity.prevPosY, entity.prevPosZ};
    }

    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        updateAndRender(event.getPartialTicks(), Minecraft.getSystemTime(), Minecraft.getMinecraft().isGamePaused());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) unloadRender();
    }
}
