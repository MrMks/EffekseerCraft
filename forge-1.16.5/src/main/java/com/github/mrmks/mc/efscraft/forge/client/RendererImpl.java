package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.common.IEfsEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.eventbus.api.Event;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl {
    protected RendererImpl() {}

    void drawEffect(EfsRenderEvent event, EfsProgram drawer) {

        if (!Minecraft.useFancyGraphics()) {
            if (event.getPhase() == IEfsEvent.Phase.END) {
                Framebuffer framebuffer = Minecraft.getInstance().getMainRenderTarget();
                drawer.setBackground(framebuffer.getColorTextureId(), false);
                drawer.setDepth(framebuffer.getDepthTextureId(), false);
                drawer.draw();
                drawer.unsetBackground();
                drawer.unsetDepth();
            }
        } else {

            if (!apiSupport)
                return;

            glGetIntegerv(GL_VIEWPORT, INT_16);
            int w = INT_16.get(2), h = INT_16.get(3);
            INT_16.clear();
            if (drawers == null) {
                drawers = new DrawerNonTransparency();
                drawers.attach();
            }
            drawers.drawEffect(w, h, event.getPhase() == IEfsEvent.Phase.START, drawer);
        }

    }

    private Drawer drawers = null;

    void cleanup() {
        drawers.detach();
        drawers.cleanup();
        drawers = null;
    }

    public static class RenderParticleEvent extends Event {
        final Matrix4f cam, proj;
        final ActiveRenderInfo info;
        final float partial;
        final long nano;
        final boolean prev;
        final boolean shader;
        public RenderParticleEvent(float partial, long nano, Matrix4f cam, Matrix4f proj, ActiveRenderInfo info, boolean prev, boolean shader) {
            this.partial = partial;
            this.nano = nano;
            this.cam = cam;
            this.proj = proj;
            this.info = info;
            this.prev = prev;
            this.shader = shader;
        }
    }

    private interface Drawer {
        default void attach() {}
        void drawEffect(int w, int h, boolean prev, EfsProgram drawer);
        default void detach() {}
        void cleanup();
    }

    private static class DrawerNonTransparency implements Drawer {

        private int workingFBO, depthAttach0, depthAttach1;
        private int lastWidth, lastHeight;
        private int texColor, texDepth;

        private void resize(int w, int h) {

            if (lastWidth == w && lastHeight == h)
                return;

            lastWidth = w; lastHeight = h;

            if (workingFBO <= 0) {
                workingFBO = glGenFramebuffers();
                depthAttach0 = glGenRenderbuffers();
                depthAttach1 = glGenRenderbuffers();

                int[] tex = new int[2];
                glGenTextures(tex);
                texColor = tex[0];
                texDepth = tex[1];
            }

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach0);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach1);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glBindRenderbuffer(GL_RENDERBUFFER, 0);

            int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D, texColor);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            glBindTexture(GL_TEXTURE_2D, texDepth);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
            glBindTexture(GL_TEXTURE_2D, originTex);
        }

        @Override
        public void drawEffect(int w, int h, boolean prev, EfsProgram drawer) {
            resize(w, h);

            int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

            int mainFBO = Minecraft.getInstance().getMainRenderTarget().frameBufferId;

            if (prev)
            {
                // copy depth buffer
                glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            }
            else
            {
                // backup depth buffer
                glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, texColor);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                glBindTexture(GL_TEXTURE_2D, texDepth);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                glBindTexture(GL_TEXTURE_2D, originTex);

                // use copied depth buffer
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainFBO);
                glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw effects
                drawer.setBackground(texColor, false);
                drawer.setDepth(texDepth, false);
                drawer.draw();
                drawer.unsetBackground();
                drawer.unsetDepth();

                // restore depth buffer
                glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            }

            glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
        }

        @Override
        public void cleanup() {
            if (workingFBO > 0)
                glDeleteFramebuffers(workingFBO);

            if (depthAttach0 > 0)
                glDeleteRenderbuffers(depthAttach0);

            if (depthAttach1 > 0)
                glDeleteRenderbuffers(depthAttach1);

            if (texColor > 0)
                glDeleteTextures(new int[] {texColor, texDepth});
        }
    }
}
