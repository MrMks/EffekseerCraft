package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.FloatBuffer;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    private final double[] pos = new double[3];

    @Override
    protected double[] getRenderViewEntityPos() {
        return pos;
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        return pos;
    }

    private Matrix4f proj, model;
    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        model.store(buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        proj.store(buffer);
    }


    private int lastWidth = -1, lastHeight = -1;
    private int depthFBO = -1, depthAttach0, depthAttach1;

    private void resize(int width, int height) {
        if (lastWidth == width && lastHeight == height)
            return;

        lastWidth = width; lastHeight = height;

        if (depthFBO < 0) {
            depthFBO = GlStateManager.glGenFramebuffers();
            depthAttach0 = GlStateManager.glGenRenderbuffers();
            depthAttach1 = GlStateManager.glGenRenderbuffers();
        }

        glBindRenderbuffer(GL_RENDERBUFFER, depthAttach0);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, depthAttach1);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);

        glBindFramebuffer(GL_FRAMEBUFFER, depthFBO);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @SubscribeEvent
    public void renderWorldStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

        if (Minecraft.useShaderTransparency()) {
            RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();

            int w, h;
            resize(w = framebuffer.viewWidth, h = framebuffer.viewHeight);

            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer.frameBufferId);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthFBO);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"removal"})
    public void renderWorldLast(RenderLevelLastEvent event) {
        proj = event.getProjectionMatrix().copy();
        model = event.getPoseStack().last().pose().copy();

        Minecraft minecraft = Minecraft.getInstance();

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 vec3 = camera.getPosition();
        pos[0] = vec3.x; pos[1] = vec3.y; pos[2] = vec3.z;

        RenderTarget mainFBO = minecraft.getMainRenderTarget();
        int w = mainFBO.viewWidth, h = mainFBO.viewHeight;
        if (Minecraft.useShaderTransparency()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO.frameBufferId);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthFBO);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

            glBindFramebuffer(GL_READ_FRAMEBUFFER, depthFBO);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainFBO.frameBufferId);
            glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        } else {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainFBO.frameBufferId);
        }

        updateAndRender(event.getPartialTick(), event.getStartNanos(), 1_000_000_000, minecraft.isPaused());

        if (Minecraft.useShaderTransparency()) {
            glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

            glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, mainFBO.frameBufferId);
    }
}
