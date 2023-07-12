package com.github.mrmks.mc.efscraft.forge.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@SuppressWarnings("SameParameterValue")
public class GLHelper {
    static final boolean apiSupport = GlStateManager.supportsFramebufferBlit();

    private static final ByteBuffer BYTE_64 = BufferUtils.createByteBuffer(64);
    static final IntBuffer INT_16 = BYTE_64.asIntBuffer();
    static final FloatBuffer FLOAT_16 = BYTE_64.asFloatBuffer();

    // commons;
    static final int GL_FLOAT = GL20.GL_FLOAT;
    static final int GL_UNSIGNED_BYTE = GL20.GL_UNSIGNED_BYTE;
    static final int GL_ZERO = GL20.GL_ZERO;
    static final int GL_ONE = GL20.GL_ONE;

    static final int GL_NO_ERROR = GL20.GL_NO_ERROR;
    static int glGetError() {
        return GL20.glGetError();
    }

    // bits;
    static final int GL_COLOR_BUFFER_BIT = GL20.GL_COLOR_BUFFER_BIT;
    static final int GL_DEPTH_BUFFER_BIT = GL20.GL_DEPTH_BUFFER_BIT;
    static final int GL_STENCIL_BUFFER_BIT = GL20.GL_STENCIL_BUFFER_BIT;

    // filter
    static final int GL_NEAREST = GL20.GL_NEAREST;
    static final int GL_LINEAR = GL20.GL_LINEAR;

    // buffers
    static final int GL_ARRAY_BUFFER = GL20.GL_ARRAY_BUFFER;
    static final int GL_STATIC_DRAW = GL20.GL_STATIC_DRAW;
    static int glGenBuffers() {
        return GL20.glGenBuffers();
    }

    static void glDeleteBuffers(int buffer) {
        GL20.glDeleteBuffers(buffer);
    }

    static void glBindBuffer(int target, int buffer) {
        GL20.glBindBuffer(target, buffer);
    }

    static void glBufferData(int target, float[] data, int usage) {
        GL20.glBufferData(target, data, usage);
    }

    static void glVertexAttribPointer(int index, int size, int format, boolean normalized, int stride, long offset) {
        GL20.glVertexAttribPointer(index, size, format, normalized, stride, offset);
    }

    static void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    static void glDisableVertexAttribArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    // draws
    static final int GL_TRIANGLE_FAN = GL20.GL_TRIANGLE_FAN;
    static void glDrawArrays(int type, int first, int count) {
        GL20.glDrawArrays(type, first, count);
    }

    // clears
    static void glClearColor(float red, float blue, float green, float alpha) {
        GL20.glClearColor(red, blue, green, alpha);
    }

    static void glClear(int masks) {
        GL20.glClear(masks);
    }

    // enable and disables
    static final int GL_DEPTH_TEST = GL20.GL_DEPTH_TEST;
    static final int GL_STENCIL_TEST = GL20.GL_STENCIL_TEST;
    static final int GL_BLEND = GL20.GL_BLEND;
    static final int GL_FOG = GL20.GL_FOG;
    static final int GL_CULL_FACE = GL20.GL_CULL_FACE;
    static final int GL_LIGHTING = GL20.GL_LIGHTING;
    static final int GL_COLOR_MATERIAL = GL20.GL_COLOR_MATERIAL;
    static final int GL_ALPHA_TEST = GL20.GL_ALPHA_TEST;

    static void glEnable(int cap) {
        switch (cap) {
            case GL_DEPTH_TEST:
                RenderSystem.enableDepthTest(); break;
            case GL_BLEND:
                RenderSystem.enableBlend(); break;
            case GL_CULL_FACE:
                RenderSystem.enableCull(); break;
            case GL_ALPHA_TEST:
                RenderSystem.enableAlphaTest(); break;
            case GL_FOG:
                RenderSystem.enableFog(); break;
            case GL_LIGHTING:
                RenderSystem.enableLighting(); break;
            case GL_COLOR_MATERIAL:
                RenderSystem.enableColorMaterial(); break;
            default:
                GL20.glEnable(cap);
        }
    }

    static void glDisable(int cap) {
        switch (cap) {
            case GL_DEPTH_TEST:
                RenderSystem.disableDepthTest(); break;
            case GL_BLEND:
                RenderSystem.disableBlend(); break;
            case GL_CULL_FACE:
                RenderSystem.disableCull(); break;
            case GL_ALPHA_TEST:
                RenderSystem.disableAlphaTest(); break;
            case GL_FOG:
                RenderSystem.disableFog(); break;
            case GL_LIGHTING:
                RenderSystem.disableLighting(); break;
            case GL_COLOR_MATERIAL:
                RenderSystem.disableColorMaterial(); break;
            default:
                GL20.glDisable(cap);
        }
    }

    static boolean glIsEnabled(int cap) {
        return GL20.glIsEnabled(cap);
    }

    static void glReadBuffer(int src) {
        GL20.glReadBuffer(src);
    }

    static void glDrawBuffer(int buf) {
        GL20.glDrawBuffer(buf);
    }

    static void glDrawBuffers(int[] bufs) {
        GL20.glDrawBuffers(bufs);
    }

    // colors
    static void glColorMask(boolean red, boolean blue, boolean green, boolean alpha) {
        GL20.glColorMask(red, blue, green, alpha);
    }

    // depth
    static void glDepthMask(boolean mask) {
        GlStateManager._depthMask(mask);
    }

    static void glDepthFunc(int func) {
        GlStateManager._depthFunc(func);
    }

    // blend
    static final int GL_SRC_ALPHA = GL20.GL_SRC_ALPHA;
    static final int GL_ONE_MINUS_SRC_ALPHA = GL20.GL_ONE_MINUS_SRC_ALPHA;
    static void glBlendFuncSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        GlStateManager._blendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
    }

    // stencil
    static final int GL_KEEP = GL20.GL_KEEP;
    static final int GL_REPLACE = GL20.GL_REPLACE;
    static final int GL_EQUAL = GL20.GL_EQUAL;
    static final int GL_NOTEQUAL = GL20.GL_NOTEQUAL;
    static final int GL_ALWAYS = GL20.GL_ALWAYS;
    static void glStencilMask(int mask) {
        GlStateManager._stencilMask(mask);
    }

    static void glStencilFunc(int func, int ref, int mask) {
        GlStateManager._stencilFunc(func, ref, mask);
    }

    static void glStencilOp(int sfail, int dfail, int pass) {
        GlStateManager._stencilOp(sfail, dfail, pass);
    }

    // gets
    static final int GL_VIEWPORT = GL20.GL_VIEWPORT;
    static final int GL_ACTIVE_TEXTURE = GL20.GL_ACTIVE_TEXTURE;
    static final int GL_TEXTURE_BINDING_2D = GL20.GL_TEXTURE_BINDING_2D;
    static final int GL_READ_BUFFER = GL20.GL_READ_BUFFER;
    static final int GL_DRAW_BUFFER = GL20.GL_DRAW_BUFFER;
    static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    static final int GL_CURRENT_PROGRAM = GL20.GL_CURRENT_PROGRAM;
    static final int GL_COLOR_CLEAR_VALUE = GL20.GL_COLOR_CLEAR_VALUE;
    static int glGetInteger(int pname) {
        return GL20.glGetInteger(pname);
    }

    static void glGetIntegerv(int pname, IntBuffer params) {
        GL20.glGetIntegerv(pname, params);
    }

    static void glGetFloatv(int pname, FloatBuffer params) {
        GL20.glGetFloatv(pname, params);
    }

    // framebuffers
    static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    static final int GL_COLOR_ATTACHMENT0 = GL30.GL_COLOR_ATTACHMENT0;
    static final int GL_COLOR_ATTACHMENT1 = GL30.GL_COLOR_ATTACHMENT1;
    static final int GL_DEPTH_ATTACHMENT = GL30.GL_DEPTH_ATTACHMENT;
    static final int GL_STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
    static final int GL_FRAMEBUFFER_COMPLETE = GL30.GL_FRAMEBUFFER_COMPLETE;
    static int glGenFramebuffers() {
        return GlStateManager.glGenFramebuffers();
    }

    static void glDeleteFramebuffers(int pname) {
        GlStateManager._glDeleteFramebuffers(pname);
    }

    static void glBindFramebuffer(int target, int pname) {
        GlStateManager._glBindFramebuffer(target, pname);
    }

    static void glBlitFramebuffer(int x0, int y0, int w0, int h0, int x1, int y1, int w1, int h1, int mask, int filter) {
        GlStateManager._glBlitFrameBuffer(x0, y0, w0, h0, x1, y1, w1, h1, mask, filter);
    }

    static final int GL_RENDERBUFFER = GL30.GL_RENDERBUFFER;
    static final int GL_DEPTH_COMPONENT24 = GL20.GL_DEPTH_COMPONENT24;
    static final int GL_DEPTH24_STENCIL8 = GL30.GL_DEPTH24_STENCIL8;

    static int glGenRenderbuffers() {
        return GL30.glGenRenderbuffers();
    }
    static void glGenRenderbuffers(int[] names) {
        GL30.glGenRenderbuffers(names);
    }

    static void glDeleteRenderbuffers(int name) {
        GL30.glDeleteRenderbuffers(name);
    }

    static void glDeleteRenderbuffers(int[] name) {
        GL30.glDeleteRenderbuffers(name);
    }

    static void glBindRenderbuffer(int target, int pname) {
        GL30.glBindRenderbuffer(target, pname);
    }

    static void glRenderbufferStorage(int target, int internalformat, int w, int h) {
        GL30.glRenderbufferStorage(target, internalformat, w, h);
    }

    static void glFramebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
        GlStateManager._glFramebufferTexture2D(target, attachment, texTarget, texture, level);
    }

    static void glFramebufferRenderbuffer(int target, int attachment, int rboTarget, int buffer) {
        GL30.glFramebufferRenderbuffer(target, attachment, rboTarget, buffer);
    }

    static int glCheckFramebufferStatus(int target) {
        return GL30.glCheckFramebufferStatus(target);
    }

    // textures
    static final int GL_TEXTURE0 = GL20.GL_TEXTURE0;
    static final int GL_TEXTURE_2D = GL20.GL_TEXTURE_2D;
    static final int GL_TEXTURE_MIN_FILTER = GL20.GL_TEXTURE_MIN_FILTER;
    static final int GL_TEXTURE_MAG_FILTER = GL20.GL_TEXTURE_MAG_FILTER;
    static final int GL_RGBA8 = GL20.GL_RGBA8;
    static final int GL_RGBA = GL20.GL_RGBA;
    static void glGenTextures(int[] names) {
        GlStateManager._genTextures(names);
    }

    static void glDeleteTextures(int[] pnames) {
        GlStateManager._deleteTextures(pnames);
    }

    static void glActiveTexture(int unit) {
        GL20.glActiveTexture(unit);
    }

    static void glBindTexture(int target, int texture) {
        GL20.glBindTexture(target, texture);
    }

    static void glTexParameteri(int target, int pname, int param) {
        GL20.glTexParameteri(target, pname, param);
    }

    static void glTexImage2D(int target, int level, int internalformat, int w, int h, int border, int format, int type, long pixels) {
        GL20.glTexImage2D(target, level, internalformat, w, h, border, format, type, pixels);
    }

    static void glCopyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y, int w, int h) {
        GL20.glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, w, h);
    }

    // shader and programs
    static final int GL_FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
    static final int GL_VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    static final int GL_COMPILE_STATUS = GL20.GL_COMPILE_STATUS;
    static final int GL_LINK_STATUS = GL20.GL_LINK_STATUS;
    static int glCreateShader(int type) {
        return GlStateManager.glCreateShader(type);
    }

    static void glDeleteShader(int shader) {
        GlStateManager.glDeleteShader(shader);
    }

    static void glShaderSource(int shader, String source) {
        GlStateManager.glShaderSource(shader, source);
    }

    static void glCompileShader(int shader) {
        GlStateManager.glCompileShader(shader);
    }

    static int glGetShaderi(int shader, int pname) {
        return GlStateManager.glGetShaderi(shader, pname);
    }

    static String glGetShaderInfoLog(int shader) {
        return GlStateManager.glGetShaderInfoLog(shader, glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH));
    }

    static int glCreateProgram() {
        return GlStateManager.glCreateProgram();
    }

    static void glDeleteProgram(int program) {
        GlStateManager.glDeleteProgram(program);
    }

    static void glAttachShader(int program, int shader) {
        GlStateManager.glAttachShader(program, shader);
    }

    static void glBindAttribLocation(int program, int index, String name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    static int glGetUniformLocation(int program, String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    static void glUniform1i(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    static void glLinkProgram(int program) {
        GlStateManager.glLinkProgram(program);
    }

    static int glGetProgrami(int program, int pname) {
        return GlStateManager.glGetProgrami(program, pname);
    }

    static String glGetProgramInfoLog(int program) {
        return GlStateManager.glGetProgramInfoLog(program, glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH));
    }

    static void glUseProgram(int program) {
        GlStateManager._glUseProgram(program);
    }
}
