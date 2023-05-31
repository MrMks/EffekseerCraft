package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.FloatBuffer;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        return viewPos;
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        return viewPos;
    }

    private Matrix4f camMat, projMat;
    private final double[] viewPos = new double[3];

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

        if (Minecraft.useShaderTransparency()) return;

        this.camMat = event.getMatrixStack().last().pose();
        this.projMat = event.getProjectionMatrix();

        ActiveRenderInfo info = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d v3d = info.getPosition();
        viewPos[0] = v3d.x; viewPos[1] = v3d.y; viewPos[2] = v3d.z;

        updateAndRender(event.getPartialTicks(), event.getFinishTimeNano(), 1000_000_000, Minecraft.getInstance().isPaused());
    }

    @SubscribeEvent
    public void renderParticle(RenderParticleEvent event) {

//        if (!Minecraft.useShaderTransparency()) return;

        this.camMat = event.cam;
        this.projMat = event.proj;

        Vector3d v3d = event.info.getPosition();
        viewPos[0] = v3d.x; viewPos[1] = v3d.y; viewPos[2] = v3d.z;

        updateAndRender(event.partial, event.nano, 1000_000_000, Minecraft.getInstance().isPaused());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
        if (world != null && world.isClientSide())
            unloadRender();
    }

    public static class RenderParticleEvent extends Event {
        private final Matrix4f cam, proj;
        private final ActiveRenderInfo info;
        private final float partial;
        private final long nano;
        public RenderParticleEvent(float partial, long nano, Matrix4f cam, Matrix4f proj, ActiveRenderInfo info) {
            this.partial = partial;
            this.nano = nano;
            this.cam = cam;
            this.proj = proj;
            this.info = info;
        }
    }
}
