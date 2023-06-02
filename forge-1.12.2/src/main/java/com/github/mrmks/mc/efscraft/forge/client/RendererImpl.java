package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static net.minecraft.client.renderer.OpenGlHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;

class RendererImpl extends Renderer {

    private Framebuffer working = null, overlay = null;
    private int lastFramebuffer = -1;
    private final int vbo;
    private final int program;
    private final int attrPos, attrUV;
    private final int texColorBackup, texDepthBackup, texDepthWorking, texDepthOverlay;
    private final boolean translucent;

    private int lastWidth = -1, lastHeight = -1;

    RendererImpl(RenderingQueue queue, boolean translucent) {
        super(queue);
        this.translucent = translucent;

        float[] data = {
                -1f, -1f,  0,      0, 0,        // left-bottom
                -1f,  1f,  0,      0, 1,        // left-top
                +1f,  1f,  0,      1, 1,        // right-top
                +1f, -1f,  0,      1, 0,        // right-bottom
        };

        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length * 4);
        buffer.asFloatBuffer().put(data);
        buffer.position(0);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        String vs = "#version 120\n" +
                "attribute vec3 Position;\n" +
                "attribute vec2 UV;\n" +
                "varying vec2 texCoord;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(Position, 1);\n" +
                "    texCoord = UV;\n" +
                "}\n";

        String fs = "#version 120\n" +
                "uniform sampler2D backupColor;\n" +
                "uniform sampler2D backupDepth;\n" +
                "uniform sampler2D workingDepth;\n" +
                "uniform sampler2D overlayDepth;\n" +
                "varying vec2 texCoord;\n" +
                '\n' +
                "void main() {\n" +
                "    float d0 = texture2D(backupDepth, texCoord.xy).r;\n" +
                "    float d1 = texture2D(workingDepth, texCoord.xy).r;\n" +
                "    float d2 = texture2D(overlayDepth, texCoord.xy).r;\n" +
                "    gl_FragColor = texture2D(backupColor, texCoord.xy);\n" +
                "    if (d1 < d2) {\n" +
                "        gl_FragDepth = d1;\n" +
                "    } else {\n" +
                "        gl_FragDepth = d0;\n" +
                "    }\n" +
                "}\n"
                ;

        int vertShader = glCreateShader(GL_VERTEX_SHADER);
        byte[] bytes = vs.getBytes(StandardCharsets.UTF_8);
        buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
        buffer.position(0);
        OpenGlHelper.glShaderSource(vertShader, buffer);
        OpenGlHelper.glCompileShader(vertShader);

        if (glGetShaderi(vertShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(vertShader, GL_INFO_LOG_LENGTH));
        }

        int fragShader = OpenGlHelper.glCreateShader(GL_FRAGMENT_SHADER);
        bytes = fs.getBytes(StandardCharsets.UTF_8);
        buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
        buffer.position(0);
        OpenGlHelper.glShaderSource(fragShader, buffer);
        OpenGlHelper.glCompileShader(fragShader);

        if (glGetShaderi(fragShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(fragShader, GL_INFO_LOG_LENGTH));
        }

        int program = OpenGlHelper.glCreateProgram();
        OpenGlHelper.glAttachShader(program, vertShader);
        OpenGlHelper.glAttachShader(program, fragShader);
        OpenGlHelper.glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(program, GL_INFO_LOG_LENGTH));
        }

        OpenGlHelper.glDeleteShader(fragShader);
        OpenGlHelper.glDeleteShader(vertShader);

        int prevProgram = GlStateManager.glGetInteger(GL_CURRENT_PROGRAM);

        this.program = program;
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "backupColor"), 0);
        glUniform1i(glGetUniformLocation(program, "backupDepth"), 1);
        glUniform1i(glGetUniformLocation(program, "workingDepth"), 2);
        glUniform1i(glGetUniformLocation(program, "overlayDepth"), 3);
        glUseProgram(prevProgram);

        this.attrPos = glGetAttribLocation(program, "Position");
        this.attrUV = glGetAttribLocation(program, "UV");

        texColorBackup = GlStateManager.generateTexture();
        texDepthBackup = GlStateManager.generateTexture();
        texDepthOverlay = GlStateManager.generateTexture();
        texDepthWorking = GlStateManager.generateTexture();

        int current = GlStateManager.glGetInteger(GL_TEXTURE_BINDING_2D);

        GlStateManager.bindTexture(texColorBackup);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GlStateManager.bindTexture(texDepthBackup);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GlStateManager.bindTexture(texDepthOverlay);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GlStateManager.bindTexture(texDepthWorking);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        GlStateManager.bindTexture(current);
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.posX, entity.posY, entity.posZ};
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
            return null;
        else
            return new double[] {entity.prevPosX, entity.prevPosY, entity.prevPosZ};
    }

    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
    }

    private void tryResize(int w, int h) {

        if (lastWidth == w && lastHeight == h)
            return;

        lastWidth = w;
        lastHeight = h;

        if (isFramebufferEnabled() && FramebufferHelper.apiSupported) {

            int current;

            current = FramebufferHelper.getCurrentFramebuffer();

            if (working == null) {
                working = new Framebuffer(w, h, true);
                working.enableStencil();
                working.setFramebufferColor(0, 0, 0, 0);
            } else {
                working.deleteFramebuffer();
                working.createFramebuffer(w, h);
            }
            working.bindFramebuffer(true);

            if (overlay == null) {
                overlay = new Framebuffer(w, h, true);
                overlay.setFramebufferColor(0, 0, 0, 0);
            } else {
                overlay.deleteFramebuffer();
                overlay.createFramebuffer(w, h);
            }
            overlay.bindFramebuffer(true);
            glBindFramebuffer(GL_FRAMEBUFFER, current);

            current = glGetInteger(GL_TEXTURE_BINDING_2D);

            GlStateManager.bindTexture(texColorBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GlStateManager.bindTexture(texDepthBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            GlStateManager.bindTexture(texDepthWorking);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            GlStateManager.bindTexture(texDepthOverlay);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);

            GlStateManager.bindTexture(current);

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.out.println("error after efscraft resize: " + error );
            }
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderParticleEvent event) {

        Minecraft minecraft = Minecraft.getMinecraft();

        int width, height;
        tryResize(width = minecraft.displayWidth, height = minecraft.displayHeight);

        if (translucent && isFramebufferEnabled() && FramebufferHelper.apiSupported)
        {
            int current;

            if (event.prev)
            {
                current = FramebufferHelper.getCurrentFramebuffer();
                lastFramebuffer = current;

                update(event.partial, event.finishNano, 1000_000_000L, Minecraft.getMinecraft().isGamePaused());

                GlStateManager.depthMask(true);
                glStencilMask(0xff);
                working.framebufferClear();
                working.bindFramebuffer(false);
                glClear(GL_STENCIL_BUFFER_BIT);
                overlay.framebufferClear();

                FramebufferHelper.copyFrom(current, working.framebufferObject, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, width, height);
                FramebufferHelper.copyFrom(current, overlay.framebufferObject, GL_DEPTH_BUFFER_BIT, width, height);

                glBindFramebuffer(GL_FRAMEBUFFER, overlay.framebufferObject);
                GlStateManager.depthMask(true);
            }
            else
            {
                current = lastFramebuffer;
                lastFramebuffer = -1;

                GlStateManager.depthMask(false);

                FramebufferHelper.copyDepthFrom(overlay.framebufferObject, working.framebufferObject, width, height);
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                drawTexture(overlay.framebufferTexture);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                // generate stencil buffer
                glEnable(GL_STENCIL_TEST);
                glStencilMask(0xff);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

                draw();

                // use stencil test;
                glStencilMask(0x00);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                int[] restoreTex = new int[4];
                GlStateManager.setActiveTexture(defaultTexUnit);
                restoreTex[0] = glGetInteger(GL_TEXTURE_BINDING_2D);
                GlStateManager.bindTexture(texColorBackup);
                glBindFramebuffer(GL_FRAMEBUFFER, current);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                GlStateManager.setActiveTexture(defaultTexUnit + 1);
                restoreTex[1] = glGetInteger(GL_TEXTURE_BINDING_2D);
                GlStateManager.bindTexture(texDepthBackup);
                glBindFramebuffer(GL_FRAMEBUFFER, current);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                GlStateManager.setActiveTexture(defaultTexUnit + 2);
                restoreTex[2] = glGetInteger(GL_TEXTURE_BINDING_2D);
                GlStateManager.bindTexture(texDepthWorking);
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                GlStateManager.setActiveTexture(defaultTexUnit + 3);
                restoreTex[3] = glGetInteger(GL_TEXTURE_BINDING_2D);
                GlStateManager.bindTexture(texDepthOverlay);
                glBindFramebuffer(GL_FRAMEBUFFER, overlay.framebufferObject);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                // draw texture from main to working use program;
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                glUseProgram(program);

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                GL20.glVertexAttribPointer(attrPos, 3, GL_FLOAT, false, 20, 0);
                GL20.glVertexAttribPointer(attrUV, 2, GL_FLOAT, false, 20, 12);
                GL20.glEnableVertexAttribArray(attrPos);
                GL20.glEnableVertexAttribArray(attrUV);

                prevDrawTex();
                GlStateManager.enableDepth();
                GlStateManager.disableBlend();
                int depthFunc = GlStateManager.glGetInteger(GL_DEPTH_FUNC);
                GlStateManager.depthFunc(GL_ALWAYS);
                GlStateManager.depthMask(true);

                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

                GlStateManager.depthMask(false);
                GlStateManager.depthFunc(depthFunc);
                GlStateManager.enableBlend();

                postDrawTex();

                GL20.glDisableVertexAttribArray(attrPos);
                GL20.glDisableVertexAttribArray(attrUV);
                glUseProgram(0);

                for (int i = 0; i < 4; i++) {
                    GlStateManager.setActiveTexture(defaultTexUnit + i);
                    GlStateManager.bindTexture(restoreTex[i]);
                }
                GlStateManager.setActiveTexture(defaultTexUnit);

                draw();

                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                drawTexture(overlay.framebufferTexture);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                glDisable(GL_STENCIL_TEST);

                // restore stencil buffer
                FramebufferHelper.copyFrom(working.framebufferObject, current, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, width, height);
                glBindFramebuffer(GL_FRAMEBUFFER, current);
            }
        }
        else
        {
            if (!event.prev) {
                updateAndRender(event.partial, event.finishNano, 1000_000_000L, Minecraft.getMinecraft().isGamePaused());
            }
        }
    }

    private void prevDrawTex() {
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.disableFog();

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
    }

    private void postDrawTex() {
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.enableFog();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
    }

    private void drawTexture(int textureNext) {

        prevDrawTex();

        int textureId = GlStateManager.glGetInteger(GL_TEXTURE_BINDING_2D);
        if (textureNext >= 0)
            GlStateManager.bindTexture(textureNext);

        OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, vbo);
        GlStateManager.glVertexPointer(3, GL_FLOAT, 20, 0);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glTexCoordPointer(2, GL_FLOAT, 20, 12);

        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        GlStateManager.glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        GlStateManager.glDisableClientState(GL_VERTEX_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL_TEXTURE_COORD_ARRAY);

        if (textureNext >= 0)
            GlStateManager.bindTexture(textureId);

        OpenGlHelper.glBindBuffer(GL_ARRAY_BUFFER, 0);

        postDrawTex();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) unloadRender();
    }

    public void deleteFramebuffer() {
        if (overlay != null)
            overlay.deleteFramebuffer();
        if (working != null)
            working.deleteFramebuffer();
        if (vbo >= 0)
            glDeleteBuffers(vbo);
        if (program >= 0)
            glDeleteProgram(program);
        if (texColorBackup >= 0)
            GlStateManager.deleteTexture(texColorBackup);
        if (texDepthBackup >= 0)
            GlStateManager.deleteTexture(texDepthBackup);
        if (texDepthWorking >= 0)
            GlStateManager.deleteTexture(texDepthWorking);
        if (texDepthOverlay >= 0)
            GlStateManager.deleteTexture(texDepthOverlay);
    }

    public static class RenderParticleEvent extends Event {
        private final int pass;
        private final float partial;
        private final long finishNano;
        private final boolean prev;

        RenderParticleEvent(int pass, float partial, long finishNano, boolean prev) {
            this.pass = pass;
            this.partial = partial;
            this.finishNano = finishNano;
            this.prev = prev;
        }
    }

    private static class FramebufferHelper {

        static final boolean apiSupported;
        static final boolean apiGL30;

        static final int GL_FRAMEBUFFER_BINDING;
        static final int GL_READ_FRAMEBUFFER;
        static final int GL_DRAW_FRAMEBUFFER;

        static final int GL_STENCIL_ATTACHMENT;
        static final int GL_DEPTH_STENCIL_ATTACHMENT;
        static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME;

        static final int GL_STENCIL_INDEX8;

        static {
            ContextCapabilities cap = GLContext.getCapabilities();

            apiGL30 = cap.OpenGL30;
            apiSupported = OpenGlHelper.framebufferSupported && (cap.OpenGL30 || cap.GL_ARB_framebuffer_object);

            GL_FRAMEBUFFER_BINDING = apiSupported ? (apiGL30 ? GL30.GL_FRAMEBUFFER_BINDING : ARBFramebufferObject.GL_FRAMEBUFFER_BINDING) : -1;
            GL_READ_FRAMEBUFFER = apiSupported ? (apiGL30 ? GL30.GL_READ_FRAMEBUFFER : ARBFramebufferObject.GL_READ_FRAMEBUFFER) : -1;
            GL_DRAW_FRAMEBUFFER = apiSupported ? (apiGL30 ? GL30.GL_DRAW_FRAMEBUFFER : ARBFramebufferObject.GL_DRAW_FRAMEBUFFER) : -1;

            GL_STENCIL_ATTACHMENT = apiSupported ? (apiGL30 ? GL30.GL_STENCIL_ATTACHMENT : ARBFramebufferObject.GL_STENCIL_ATTACHMENT) : -1;
            GL_DEPTH_STENCIL_ATTACHMENT = apiSupported ? (apiGL30 ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : ARBFramebufferObject.GL_DEPTH_STENCIL_ATTACHMENT) : -1;
            GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME =
                    apiSupported ? (apiGL30 ? GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME : ARBFramebufferObject.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) : -1;

            GL_STENCIL_INDEX8 = apiSupported ? (apiGL30 ? GL30.GL_STENCIL_INDEX8 : ARBFramebufferObject.GL_STENCIL_INDEX8) : -1;
        }

        static void copyDepthFrom(int src, int tar, int width, int height) {
            copyFrom(src, tar, GL_DEPTH_BUFFER_BIT, width, height);
        }

        static void copyColorDepthFrom(int src, int tar, int width, int height) {
            copyFrom(src, tar, GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT, width, height);
        }

        static void copyFrom(int src, int tar, int masks, int width, int height) {

            int id;
            if (apiGL30) {
                id = GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING);
                GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
                GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

                GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);
            } else {
                id = GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING);
                ARBFramebufferObject.glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
                ARBFramebufferObject.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

                ARBFramebufferObject.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);
            }
            OpenGlHelper.glBindFramebuffer(GL_FRAMEBUFFER, id);
        }

        static int getCurrentFramebuffer() {
            return apiSupported ? GlStateManager.glGetInteger(GL_FRAMEBUFFER_BINDING) : -1;
        }

        static int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname) {
            if (apiSupported) {
                return apiGL30 ? GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname) : ARBFramebufferObject.glGetFramebufferAttachmentParameteri(target, attachment, pname);
            } else {
                return -1;
            }
        }
    }

}
