package com.github.mrmks.mc.efscraft.forge.client;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class GLHelper {

    static final FloatBuffer FLOAT_16 = BufferUtils.createFloatBuffer(16);

    static final int GL_FRAMEBUFFER = GL30.GL_FRAMEBUFFER;
    static final int GL_READ_FRAMEBUFFER = GL30.GL_READ_FRAMEBUFFER;
    static final int GL_DRAW_FRAMEBUFFER = GL30.GL_DRAW_FRAMEBUFFER;

    static final int GL_DEPTH_COMPONENT24 = GL30.GL_DEPTH_COMPONENT24;
    static final int GL_DEPTH_ATTACHMENT = GL30.GL_DEPTH_ATTACHMENT;

    static final int GL_RENDERBUFFER = GL30.GL_RENDERBUFFER;

    static final int GL_DEPTH_BUFFER_BIT = GL30.GL_DEPTH_BUFFER_BIT;
    static final int GL_NEAREST = GL30.GL_NEAREST;

    static int glGenFramebuffers() {
        return GlStateManager.glGenFramebuffers();
    }

    static void glDeleteFramebuffers(int fbo) {
        GlStateManager._glDeleteFramebuffers(fbo);
    }

    static void glBindFramebuffer(int target, int id) {
        GlStateManager._glBindFramebuffer(target, id);
    }

    static void glFramebufferRenderbuffer(int target, int attachment, int type, int id) {
        GlStateManager._glFramebufferRenderbuffer(target, attachment, type, id);
    }

    static int glGenRenderbuffers() {
        return GlStateManager.glGenRenderbuffers();
    }

    static void glDeleteRenderbuffers(int rbo) {
        GlStateManager._glDeleteRenderbuffers(rbo);
    }

    static void glBindRenderbuffer(int target, int id) {
        GlStateManager._glBindRenderbuffer(target, id);
    }

    static void glRenderbufferStorage(int target, int internalformat, int w, int h) {
        GlStateManager._glRenderbufferStorage(target, internalformat, w, h);
    }

    static void glBlitFramebuffer(int x0, int y0, int w0, int h0, int x1, int y1, int w1, int h1, int masks, int filter) {
        GlStateManager._glBlitFrameBuffer(x0, y0, w0, h0, x1, y1, w1, h1, masks, filter);
    }
}
