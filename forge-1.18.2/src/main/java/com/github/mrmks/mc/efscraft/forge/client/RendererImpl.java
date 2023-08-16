package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.common.IEfsEvent;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl {
    protected RendererImpl() {}

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

    void drawEffect(IEfsEvent.Phase phase, Runnable drawer) {
        if (phase == IEfsEvent.Phase.START) {
            drawEffectPrev(drawer);
        } else {
            drawEffectPost(drawer);
        }
    }

    private void drawEffectPrev(Runnable drawer) {
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

    private void drawEffectPost(Runnable drawer) {
        Minecraft minecraft = Minecraft.getInstance();

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

        drawer.run();

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
