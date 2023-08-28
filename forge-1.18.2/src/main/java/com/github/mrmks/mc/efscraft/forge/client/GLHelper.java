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

    // textures
    static final int GL_TEXTURE_2D = GL30.GL_TEXTURE_2D;
    static final int GL_RGBA8 = GL30.GL_RGBA8;
    static final int GL_RGBA = GL30.GL_RGBA;
    static final int GL_DEPTH_COMPONENT = GL30.GL_DEPTH_COMPONENT;
    static final int GL_UNSIGNED_BYTE = GL30.GL_UNSIGNED_BYTE;
    static final int GL_FLOAT = GL30.GL_FLOAT;
    static final int GL_TEXTURE_MIN_FILTER = GL30.GL_TEXTURE_MIN_FILTER;
    static final int GL_TEXTURE_MAG_FILTER = GL30.GL_TEXTURE_MAG_FILTER;
    static final int GL_LINEAR = GL30.GL_LINEAR;

    static int glGenTextures() {
        return GlStateManager._genTexture();
    }

    static void glDeleteTextures(int name) {
        GlStateManager._deleteTexture(name);
    }

    static void glBindTexture(int target, int id) {
        if (target == GL_TEXTURE_2D)
            GlStateManager._bindTexture(id);
        else
            GL30.glBindTexture(target, id);
    }

    static void glTexImage2D(int target, int level, int internal, int w, int h, int border, int format, int type, int pointer) {
        if (pointer == 0)
            GlStateManager._texImage2D(target, level, internal, w, h, border, format, type, null);
        else
            GL30.glTexImage2D(target, level, internal, w, h, border, format, type, pointer);
    }

    static void glTexParameteri(int target, int pname, int v) {
        GlStateManager._texParameter(target, pname, v);
    }

    static void glCopyTexSubImage2D(int target, int level, int xo, int yo, int x, int y, int w, int h) {
        GL30.glCopyTexSubImage2D(target, level, xo, yo, x, y, w, h);
    }

    static final int GL_TEXTURE_BINDING_2D = GL30.GL_TEXTURE_BINDING_2D;
    static int glGetInteger(int target) {
        return GlStateManager._getInteger(target);
    }
}
