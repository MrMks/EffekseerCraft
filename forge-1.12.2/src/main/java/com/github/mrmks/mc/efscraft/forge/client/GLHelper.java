package com.github.mrmks.mc.efscraft.forge.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL11.*;

class GLHelper {

    // buffer related;
    static final int GL_ARRAY_BUFFER = OpenGlHelper.GL_ARRAY_BUFFER;
    static final int GL_STATIC_DRAW = OpenGlHelper.GL_STATIC_DRAW;
    static final int GL_DRAW_BUFFER0 = GL20.GL_DRAW_BUFFER0;

    // shaders and programs
    static final int GL_VERTEX_SHADER = OpenGlHelper.GL_VERTEX_SHADER;
    static final int GL_FRAGMENT_SHADER = OpenGlHelper.GL_FRAGMENT_SHADER;
    static final int GL_COMPILE_STATUS = OpenGlHelper.GL_COMPILE_STATUS;
    static final int GL_LINK_STATUS = OpenGlHelper.GL_LINK_STATUS;
    static final int GL_INFO_LOG_LENGTH = 0x8B84;
    static final int GL_CURRENT_PROGRAM = 0x8B8D;

    // textures
    static final int GL_TEXTURE0 = OpenGlHelper.defaultTexUnit;
    static final int GL_DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
    static final int GL_ACTIVE_TEXTURE = GL13.GL_ACTIVE_TEXTURE;

    // framebuffers
    static final int GL_FRAMEBUFFER = OpenGlHelper.GL_FRAMEBUFFER;
    static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    static final int GL_DRAW_FRAMEBUFFER_BINDING = GL_FRAMEBUFFER_BINDING;
    static final int GL_COLOR_ATTACHMENT0 = OpenGlHelper.GL_COLOR_ATTACHMENT0;

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

    // statues management
    static void glEnableMC(int cap) {
        switch (cap) {
            case GL_ALPHA_TEST:         GlStateManager.enableAlpha(); break;
            case GL_BLEND:              GlStateManager.enableBlend(); break;
            case GL_COLOR_MATERIAL:     GlStateManager.enableColorMaterial(); break;
            case GL_COLOR_LOGIC_OP:     GlStateManager.enableColorLogic();
            case GL_CULL_FACE:          GlStateManager.enableCull(); break;
            case GL_DEPTH_TEST:         GlStateManager.enableDepth(); break;
            case GL_FOG:                GlStateManager.enableFog(); break;
            case GL_LIGHTING:           GlStateManager.enableLighting(); break;
            default:                    GL11.glEnable(cap); break;
        }
    }

    static void glDisableMC(int cap) {
        switch (cap) {
            case GL_ALPHA_TEST:         GlStateManager.disableAlpha(); break;
            case GL_BLEND:              GlStateManager.disableBlend(); break;
            case GL_COLOR_MATERIAL:     GlStateManager.disableColorMaterial(); break;
            case GL_COLOR_LOGIC_OP:     GlStateManager.disableColorLogic();
            case GL_CULL_FACE:          GlStateManager.disableCull(); break;
            case GL_DEPTH_TEST:         GlStateManager.disableDepth(); break;
            case GL_FOG:                GlStateManager.disableFog(); break;
            case GL_LIGHTING:           GlStateManager.disableLighting(); break;
            default:                    GL11.glDisable(cap); break;
        }
    }

    static void glDepthMaskMC(boolean mask) {
        GlStateManager.depthMask(mask);
    }

    static void glDepthFuncMC(int func) {
        GlStateManager.depthFunc(func);
    }

    static void glBlendFuncMC(int src, int tar) {
        GlStateManager.blendFunc(src, tar);
    }

    static void glBlendFuncSeparateMC(int srcColor, int tarColor, int srcAlpha, int tarAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcColor, tarColor, srcAlpha, tarAlpha);
    }

    // buffers
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

    static void glDrawBuffers(IntBuffer bufs) {
        GL20.glDrawBuffers(bufs);
    }

    // shaders
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

    // textures
    static void glBindTextureMC(int target, int texture) {
        if (target == GL_TEXTURE_2D) {
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

}
