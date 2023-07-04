package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    private int lastWidth = -1, lastHeight = -1;
    private int depthFBO = -1, depthAttach0, depthAttach1;

    private void resize(int width, int height) {
        if (lastWidth == width && lastHeight == height)
            return;

        lastWidth = width; lastHeight = height;

        if (depthFBO < 0) {
            depthFBO = glGenFramebuffers();
            depthAttach0 = glGenRenderbuffers();
            depthAttach1 = glGenRenderbuffers();
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

        Minecraft minecraft = Minecraft.getInstance();

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 vec3 = camera.getPosition();

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

        com.github.mrmks.mc.efscraft.math.Matrix4f matView, matProj;
        Vec3f vPos;
        {
            float[] floats = new float[16];
            FLOAT_16.clear();
            event.getPoseStack().last().pose().store(FLOAT_16);
            FLOAT_16.get(floats);
            matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            FLOAT_16.clear();
            event.getProjectionMatrix().store(FLOAT_16);
            FLOAT_16.get(floats);
            matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            vPos = new Vec3f(vec3.x, vec3.y, vec3.z);
        }

        updateAndRender(event.getStartNanos(), 1_000_000_000, minecraft.isPaused(), matView, vPos, vPos, 0, matProj);

        if (Minecraft.useShaderTransparency()) {
            glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

            glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, mainFBO.frameBufferId);
    }

    void closeResources() {
        if (depthFBO > 0) {
            glDeleteFramebuffers(depthFBO);
            glDeleteRenderbuffers(depthAttach0);
            glDeleteRenderbuffers(depthAttach1);
        }
    }
}
