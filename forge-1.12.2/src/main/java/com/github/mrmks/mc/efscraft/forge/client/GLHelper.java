package com.github.mrmks.mc.efscraft.forge.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"SameParameterValue"})
class GLHelper {

    // commons
    static final int GL_FALSE = GL11.GL_FALSE;

    // filters
    static final int GL_NEAREST = GL11.GL_NEAREST;
    static final int GL_LINEAR = GL11.GL_LINEAR;

    // formats
    static final int GL_RGBA = GL11.GL_RGBA;
    static final int GL_RGBA16F = GL30.GL_RGBA16F;
    static final int GL_DEPTH_COMPONENT = GL11.GL_DEPTH_COMPONENT;
    static final int GL_DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
    static final int GL_DEPTH24_STENCIL8 = GL30.GL_DEPTH24_STENCIL8;

    // types
    static final int GL_FLOAT = GL11.GL_FLOAT;
    static final int GL_UNSIGNED_INT = GL11.GL_UNSIGNED_INT;

    // masks
    static final int GL_COLOR_BUFFER_BIT = GL11.GL_COLOR_BUFFER_BIT;
    static final int GL_DEPTH_BUFFER_BIT = GL11.GL_DEPTH_BUFFER_BIT;
    static final int GL_STENCIL_BUFFER_BIT = GL11.GL_STENCIL_BUFFER_BIT;

    // factors
    static final int GL_ONE = GL11.GL_ONE;
    static final int GL_SRC_ALPHA = GL11.GL_SRC_ALPHA;
    static final int GL_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;

    // funcs
    static final int GL_ALWAYS = GL11.GL_ALWAYS;
    static final int GL_NOTEQUAL = GL11.GL_NOTEQUAL;
    static final int GL_LEQUAL = GL11.GL_LEQUAL;

    // internal formats

    private static final ByteBuffer BYTE_64 = BufferUtils.createByteBuffer(64);
    static final IntBuffer INT_16 = BYTE_64.asIntBuffer();
    static final FloatBuffer FLOAT_16 = BYTE_64.asFloatBuffer();

    // vertex attrib buffer pointer
    private static final boolean shaderSupport;
    private static final boolean shaderARB;
    private static final boolean framebufferSupport;
    private static final byte framebufferMODE;
    private static final boolean overwholeSupport;

    static {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        shaderSupport = capabilities.OpenGL21 || capabilities.GL_ARB_vertex_shader && capabilities.GL_ARB_fragment_shader && capabilities.GL_ARB_shader_objects;
        shaderARB = !capabilities.OpenGL21;

        byte mode = -1;
        if (capabilities.OpenGL30) mode = 0;
        else if (capabilities.GL_ARB_framebuffer_object) mode = 1;
        else if (capabilities.GL_EXT_framebuffer_object && capabilities.GL_EXT_framebuffer_blit) mode = 2;

        framebufferMODE = mode;
        framebufferSupport = mode >= 0;

        overwholeSupport = shaderSupport && framebufferSupport;
    }

    static boolean openglSupported() {
        return overwholeSupport;
    }

    // gets
    static final int GL_MODELVIEW_MATRIX = GL11.GL_MODELVIEW_MATRIX;
    static final int GL_PROJECTION_MATRIX = GL11.GL_PROJECTION_MATRIX;
    static final int GL_VIEWPORT = GL11.GL_VIEWPORT;
    static final int GL_COLOR_CLEAR_VALUE = GL11.GL_COLOR_CLEAR_VALUE;
    static int glGetInteger(int target) {
        return GlStateManager.glGetInteger(target);
    }

    static void glGetInteger(int target, IntBuffer buffer) {
        GlStateManager.glGetInteger(target, buffer);
    }

    static void glGetFloat(int target, FloatBuffer buffer) {
        GL11.glGetFloat(target, buffer);
    }

    static final int GL_NO_ERROR = GL11.GL_NO_ERROR;
    static int glGetError() {
        return GL11.glGetError();
    }

    // statues management
    static final int GL_ALPHA_TEST = GL11.GL_ALPHA_TEST;
    static final int GL_BLEND = GL11.GL_BLEND;
    static final int GL_COLOR_MATERIAL = GL11.GL_COLOR_MATERIAL;
    static final int GL_COLOR_LOGIC_OP = GL11.GL_COLOR_LOGIC_OP;
    static final int GL_CULL_FACE = GL11.GL_CULL_FACE;
    static final int GL_DEPTH_TEST = GL11.GL_DEPTH_TEST;
    static final int GL_FOG = GL11.GL_FOG;
    static final int GL_LIGHTING = GL11.GL_LIGHTING;
    static final int GL_STENCIL_TEST = GL11.GL_STENCIL_TEST;

    static boolean glIsEnabled(int cap) {
        return GL11.glIsEnabled(cap);
    }

    static void glEnable(int cap) {
        switch (cap) {
            case GL11.GL_ALPHA_TEST:         GlStateManager.enableAlpha(); break;
            case GL11.GL_BLEND:              GlStateManager.enableBlend(); break;
            case GL11.GL_COLOR_MATERIAL:     GlStateManager.enableColorMaterial(); break;
            case GL11.GL_COLOR_LOGIC_OP:     GlStateManager.enableColorLogic();
            case GL11.GL_CULL_FACE:          GlStateManager.enableCull(); break;
            case GL11.GL_DEPTH_TEST:         GlStateManager.enableDepth(); break;
            case GL11.GL_FOG:                GlStateManager.enableFog(); break;
            case GL11.GL_LIGHTING:           GlStateManager.enableLighting(); break;
            default:                    GL11.glEnable(cap); break;
        }
    }

    static void glDisable(int cap) {
        switch (cap) {
            case GL11.GL_ALPHA_TEST:         GlStateManager.disableAlpha(); break;
            case GL11.GL_BLEND:              GlStateManager.disableBlend(); break;
            case GL11.GL_COLOR_MATERIAL:     GlStateManager.disableColorMaterial(); break;
            case GL11.GL_COLOR_LOGIC_OP:     GlStateManager.disableColorLogic();
            case GL11.GL_CULL_FACE:          GlStateManager.disableCull(); break;
            case GL11.GL_DEPTH_TEST:         GlStateManager.disableDepth(); break;
            case GL11.GL_FOG:                GlStateManager.disableFog(); break;
            case GL11.GL_LIGHTING:           GlStateManager.disableLighting(); break;
            default:                    GL11.glDisable(cap); break;
        }
    }

    static void glDepthMask(boolean mask) {
        GlStateManager.depthMask(mask);
    }

    static void glDepthFunc(int func) {
        GlStateManager.depthFunc(func);
    }

    static void glBlendFunc(int src, int tar) {
        GlStateManager.blendFunc(src, tar);
    }

    static void glBlendFuncSeparate(int srcColor, int tarColor, int srcAlpha, int tarAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcColor, tarColor, srcAlpha, tarAlpha);
    }

    static final int GL_KEEP = GL11.GL_KEEP;
    static final int GL_REPLACE = GL11.GL_REPLACE;
    static void glStencilOp(int sfail, int dfail, int pass) {
        GL11.glStencilOp(sfail, dfail, pass);
    }
    static void glStencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }
    static void glStencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    // buffers
    static final int GL_ARRAY_BUFFER = OpenGlHelper.GL_ARRAY_BUFFER;
    static final int GL_STATIC_DRAW = OpenGlHelper.GL_STATIC_DRAW;
    static int glGenBuffers() {
        return OpenGlHelper.glGenBuffers();
    }

    static void glBindBuffer(int target, int buffer) {
        OpenGlHelper.glBindBuffer(target, buffer);
    }

    static void glBufferData(int target, float[] data, int usage) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length * 4);
        buffer.asFloatBuffer().put(data);
        buffer.position(0);

        OpenGlHelper.glBufferData(target, buffer, usage);
    }

    static void glDeleteBuffers(int buffer) {
        OpenGlHelper.glDeleteBuffers(buffer);
    }

    // shaders
    static final int GL_VERTEX_SHADER = OpenGlHelper.GL_VERTEX_SHADER;
    static final int GL_FRAGMENT_SHADER = OpenGlHelper.GL_FRAGMENT_SHADER;
    static final int GL_COMPILE_STATUS = OpenGlHelper.GL_COMPILE_STATUS;
    static final int GL_INFO_LOG_LENGTH = 0x8B84;
    static int glCreateShader(int type) {
        return OpenGlHelper.glCreateShader(type);
    }

    static void glShaderSource(int shader, String source) {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).put(bytes);
        buffer.position(0);
        OpenGlHelper.glShaderSource(shader, buffer);
    }

    static void glCompileShader(int shader) {
        OpenGlHelper.glCompileShader(shader);
    }

    static void glDeleteShader(int shader) {
        OpenGlHelper.glDeleteShader(shader);
    }

    static int glGetShaderi(int shader, int pname) {
        return OpenGlHelper.glGetShaderi(shader, pname);
    }

    static String glGetShaderInfoLog(int shader) {
        return OpenGlHelper.glGetShaderInfoLog(shader, glGetShaderi(shader, GL_INFO_LOG_LENGTH));
    }

    // programs
    static final int GL_CURRENT_PROGRAM = 0x8B8D;
    static final int GL_LINK_STATUS = OpenGlHelper.GL_LINK_STATUS;
    static int glCreateProgram() {
        return OpenGlHelper.glCreateProgram();
    }

    static void glLinkProgram(int program) {
        OpenGlHelper.glLinkProgram(program);
    }

    static void glUseProgram(int program) {
        OpenGlHelper.glUseProgram(program);
    }

    static void glDeleteProgram(int program) {
        OpenGlHelper.glDeleteProgram(program);
    }

    static int glGetProgrami(int program, int pname) {
        return OpenGlHelper.glGetProgrami(program, pname);
    }

    static String glGetProgramInfoLog(int program) {
        return OpenGlHelper.glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH));
    }

    // attach shader
    static void glAttachShader(int program, int shader) {
        OpenGlHelper.glAttachShader(program, shader);
    }

    // program getters and uniforms
    static int glGetUniformLocation(int program, CharSequence name) {
        return OpenGlHelper.glGetUniformLocation(program, name);
    }

    static void glBindAttribLocation(int program, int index, CharSequence name) {
        if (shaderSupport) {
            if (!shaderARB) {
                GL20.glBindAttribLocation(program, index, name);
            } else {
                ARBVertexShader.glBindAttribLocationARB(program, index, name);
            }
        }
    }

    static int glGetAttribLocation(int program, CharSequence name) {
        return OpenGlHelper.glGetAttribLocation(program, name);
    }

    static void glUniform1i(int location, int val) {
        OpenGlHelper.glUniform1i(location, val);
    }

    // vertex attrib array
    static void glVertexAttribPointer(int index, int size, int format, boolean normalized, int stride, long offset) {
        if (shaderSupport) {
            if (!shaderARB) {
                GL20.glVertexAttribPointer(index, size, format, normalized, stride, offset);
            } else {
                ARBVertexShader.glVertexAttribPointerARB(index, size, format, normalized, stride, offset);
            }
        }
    }

    static void glEnableVertexAttribArray(int index) {
        if (shaderSupport) {
            if (!shaderARB) {
                GL20.glEnableVertexAttribArray(index);
            } else {
                ARBVertexShader.glEnableVertexAttribArrayARB(index);
            }
        }
    }

    static void glDisableVertexAttribArray(int index) {
        if (shaderSupport) {
            if (!shaderARB) {
                GL20.glDisableVertexAttribArray(index);
            } else {
                ARBVertexShader.glDisableVertexAttribArrayARB(index);
            }
        }
    }

    // framebuffers
    static final int GL_FRAMEBUFFER = OpenGlHelper.GL_FRAMEBUFFER;
    static final int GL_FRAMEBUFFER_BINDING = 0x8CA6; // get
    static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA; // get
    static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    static final int GL_DRAW_FRAMEBUFFER_BINDING = GL_FRAMEBUFFER_BINDING; // get
    static final int GL_COLOR_ATTACHMENT0 = OpenGlHelper.GL_COLOR_ATTACHMENT0;
    static final int GL_DEPTH_ATTACHMENT = OpenGlHelper.GL_DEPTH_ATTACHMENT;
    static final int GL_STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
    static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE;
    static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;

    static int glGenFramebuffers() {
        return OpenGlHelper.glGenFramebuffers();
    }

    static void glBindFramebuffer(int target, int fbo) {
        OpenGlHelper.glBindFramebuffer(target, fbo);
    }

    static void glBlitFramebuffer(int x0, int y0, int w0, int h0, int x1, int y1, int w1, int h1, int masks, int filter) {
        if (framebufferSupport) {
            if (framebufferMODE == 0) {
                GL30.glBlitFramebuffer(x0, y0, w0, h0, x1, y1, w1, h1, masks, filter);
            } else if (framebufferMODE == 1) {
                ARBFramebufferObject.glBlitFramebuffer(x0, y0, w0, h0, x1, y1, w1, h1, masks, filter);
            } else if (framebufferMODE == 2) {
                EXTFramebufferBlit.glBlitFramebufferEXT(x0, y0, w0, h0, x1, y1, w1, h1, masks, filter);
            }
        }
    }

    static int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        if (framebufferSupport) {
            if (framebufferMODE == 0) {
                return GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname);
            } else if (framebufferMODE == 1) {
                return ARBFramebufferObject.glGetFramebufferAttachmentParameteri(target, attachment, pname);
            } else if (framebufferMODE == 2) {
                return EXTFramebufferObject.glGetFramebufferAttachmentParameteriEXT(target, attachment, pname);
            }
        }

        return -1;
    }

    static void glFramebufferTexture2D(int target, int attachment, int texTarget, int tex, int level) {
        OpenGlHelper.glFramebufferTexture2D(target, attachment, texTarget, tex, level);
    }

    static final int GL_RENDERBUFFER = OpenGlHelper.GL_RENDERBUFFER;

    static int glGenRenderbuffers() {
        return OpenGlHelper.glGenRenderbuffers();
    }

    static void glBindRenderbuffer(int target, int buf) {
        OpenGlHelper.glBindRenderbuffer(target, buf);
    }

    static void glRenderbufferStorage(int target, int internalformat, int w, int h) {
        OpenGlHelper.glRenderbufferStorage(target, internalformat, w, h);
    }

    static void glFramebufferRenderbuffer(int targrt, int attachment, int bufTarget, int buf) {
        OpenGlHelper.glFramebufferRenderbuffer(targrt, attachment, bufTarget, buf);
    }

    // textures and samplers
    static final int GL_TEXTURE_2D = GL11.GL_TEXTURE_2D;
    static final int GL_TEXTURE0 = OpenGlHelper.defaultTexUnit;
    static final int GL_ACTIVE_TEXTURE = GL13.GL_ACTIVE_TEXTURE;
    static final int GL_TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
    static final int GL_TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
    static final int GL_TEXTURE_BINDING_2D = GL11.GL_TEXTURE_BINDING_2D; // get
    static void glGenTextures(IntBuffer buffer) {
        GL11.glGenTextures(buffer);
    }

    static void glDeleteTextures(int id) {
        GlStateManager.deleteTexture(id);
    }

    static void glTexParameteri(int target, int pname, int pvalue) {
        GL11.glTexParameteri(target, pname, pvalue);
    }

    static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer buffer) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, buffer);
    }

    static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pointer) {
        if (pointer == 0) {
            glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer) null);
        } else {
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pointer);
        }
    }

    static void glCopyTexSubImage2D(int target, int level, int xo, int yo, int x, int y, int w, int h) {
        GL11.glCopyTexSubImage2D(target, level, xo, yo, x, y, w, h);
    }

    static void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            GlStateManager.bindTexture(texture);
        } else {
            GL11.glBindTexture(target, texture);
        }
    }

    static void glActiveTexture(int texture) {
        GlStateManager.setActiveTexture(texture);
    }

    // client state management;
    static void glClientActiveTexture(int texture) {
        OpenGlHelper.setClientActiveTexture(texture);
    }

    // read and draw buffer, MRT
    static final int GL_READ_BUFFER = GL11.GL_READ_BUFFER;
    static final int GL_DRAW_BUFFER = GL11.GL_DRAW_BUFFER;
    static final int GL_DRAW_BUFFER0 = GL20.GL_DRAW_BUFFER0;

    static void glDrawBuffer(int target) {
        GL11.glDrawBuffer(target);
    }

    static void glDrawBuffers(IntBuffer bufs) {
        GL20.glDrawBuffers(bufs);
    }

    static void glReadBuffer(int buffer) {
        GL11.glReadBuffer(buffer);
    }

    // draws
    static final int GL_TRIANGLE_FAN = GL11.GL_TRIANGLE_FAN;
    static void glDrawArrays(int mode, int first, int count) {
        GlStateManager.glDrawArrays(mode, first, count);
    }

    static void glClearColor(float red, float green, float blue, float alpha) {
        GlStateManager.clearColor(red, green, blue, alpha);
    }

    static void glClear(int bits) {
        GlStateManager.clear(bits);
    }

}
