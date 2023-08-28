package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.common.IEfsEvent;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl {
    protected RendererImpl() {}

    private int lastWidth = -1, lastHeight = -1;
    private int depthFBO = -1, depthAttach0, depthAttach1;
    private int texColor, texDepth;

    private void resize(int width, int height) {
        if (lastWidth == width && lastHeight == height)
            return;

        lastWidth = width; lastHeight = height;

        if (depthFBO < 0) {
            depthFBO = glGenFramebuffers();
            depthAttach0 = glGenRenderbuffers();
            depthAttach1 = glGenRenderbuffers();

            texColor = glGenTextures();
            texDepth = glGenTextures();

            int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D, texColor);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, texDepth);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            glBindTexture(GL_TEXTURE_2D, originTex);
        }

        glBindRenderbuffer(GL_RENDERBUFFER, depthAttach0);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, depthAttach1);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);

        glBindFramebuffer(GL_FRAMEBUFFER, depthFBO);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    void drawEffect(IEfsEvent.Phase phase, EfsProgram drawer) {
        if (phase == IEfsEvent.Phase.START) {
            drawEffectPrev(drawer);
        } else {
            drawEffectPost(drawer);
        }
    }

    private void drawEffectPrev(EfsProgram drawer) {

        RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();

        int w, h;
        resize(w = framebuffer.viewWidth, h = framebuffer.viewHeight);

        if (Minecraft.useShaderTransparency()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer.frameBufferId);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthFBO);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
            glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    private void drawEffectPost(EfsProgram drawer) {
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
            glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO.frameBufferId);
        } else {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainFBO.frameBufferId);
        }

        int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texColor);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        glBindTexture(GL_TEXTURE_2D, texDepth);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        glBindTexture(GL_TEXTURE_2D, originTex);

        drawer.setBackground(texColor, false);
        drawer.setDepth(texDepth, false);
        drawer.draw();
        drawer.unsetBackground();
        drawer.unsetDepth();

        if (Minecraft.useShaderTransparency()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, depthFBO);
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
            glDeleteTextures(texColor);
            glDeleteTextures(texDepth);
        }
    }
}
