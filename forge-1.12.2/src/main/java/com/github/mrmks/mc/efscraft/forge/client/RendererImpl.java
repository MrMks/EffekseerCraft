package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;
import static org.lwjgl.opengl.GL11.*;

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

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
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
                "    float d0 = texture2D(backupDepth, texCoord).r;\n" +
                "    float d1 = texture2D(workingDepth, texCoord).r;\n" +
                "    float d2 = texture2D(overlayDepth, texCoord).r;\n" +
                "    gl_FragColor = texture2D(backupColor, texCoord);\n" +
                "    if (d1 < d2) {\n" +
                "        gl_FragDepth = d1;\n" +
                "    } else {\n" +
                "        gl_FragDepth = d0;\n" +
                "    }\n" +
                "}\n"
                ;

        int vertShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertShader, vs);
        glCompileShader(vertShader);

        if (glGetShaderi(vertShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(vertShader));
        }

        int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragShader, fs);
        glCompileShader(fragShader);

        if (glGetShaderi(fragShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.out.println(glGetShaderInfoLog(fragShader));
        }

        int program = glCreateProgram();
        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(program));
        }

        glDeleteShader(fragShader);
        glDeleteShader(vertShader);

        this.program = program;
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "backupColor"), 0);
        glUniform1i(glGetUniformLocation(program, "backupDepth"), 1);
        glUniform1i(glGetUniformLocation(program, "workingDepth"), 2);
        glUniform1i(glGetUniformLocation(program, "overlayDepth"), 3);

        this.attrPos = glGetAttribLocation(program, "Position");
        this.attrUV = glGetAttribLocation(program, "UV");

        texColorBackup = glGenTextures();
        texDepthBackup = glGenTextures();
        texDepthOverlay = glGenTextures();
        texDepthWorking = glGenTextures();

        int current = glGetInteger(GL_TEXTURE_BINDING_2D);

        glBindTextureMC(GL_TEXTURE_2D, texColorBackup);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTextureMC(GL_TEXTURE_2D, texDepthBackup);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTextureMC(GL_TEXTURE_2D, texDepthOverlay);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTextureMC(GL_TEXTURE_2D, texDepthWorking);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTextureMC(GL_TEXTURE_2D, current);
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
        glGetFloat(GL_MODELVIEW_MATRIX, buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        glGetFloat(GL_PROJECTION_MATRIX, buffer);
    }

    private void tryResize(int w, int h) {

        if (lastWidth == w && lastHeight == h)
            return;

        lastWidth = w;
        lastHeight = h;

        if (openglSupported()) {

            int current;

            current = glGetInteger(GL_FRAMEBUFFER_BINDING);

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

            glBindTextureMC(GL_TEXTURE_2D, texColorBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthWorking);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthOverlay);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);

            glBindTextureMC(GL_TEXTURE_2D, current);

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

        if (translucent && openglSupported())
        {
            int current;

            if (event.prev)
            {
                current = glGetInteger(GL_FRAMEBUFFER_BINDING);
                lastFramebuffer = current;

                update(event.partial, event.finishNano, 1000_000_000L, Minecraft.getMinecraft().isGamePaused());

                glDepthMaskMC(true);
                glStencilMask(0xff);
                working.framebufferClear();
                working.bindFramebuffer(false);
                glClear(GL_STENCIL_BUFFER_BIT);
                overlay.framebufferClear();

                blitFramebuffer(current, working.framebufferObject, width, height, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                blitFramebuffer(current, overlay.framebufferObject, width, height, GL_DEPTH_BUFFER_BIT);

                glBindFramebuffer(GL_FRAMEBUFFER, overlay.framebufferObject);
                glBlendFuncSeparateMC(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                glDepthMaskMC(true);
            }
            else
            {
                current = lastFramebuffer;
                lastFramebuffer = -1;

                glDepthMaskMC(false);

                blitFramebuffer(overlay.framebufferObject, working.framebufferObject, width, height, GL_DEPTH_BUFFER_BIT);
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                glBlendFuncMC(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                drawTexture(overlay.framebufferTexture);
                glBlendFuncMC(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                // generate stencil buffer
                glEnableMC(GL_STENCIL_TEST);
                glStencilMask(0xff);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

                draw();

                // use stencil test;
                glStencilMask(0x00);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                int[] restoreTex = new int[4];
                glActiveTexture(GL_TEXTURE0);
                restoreTex[0] = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTextureMC(GL_TEXTURE_2D, texColorBackup);
                glBindFramebuffer(GL_FRAMEBUFFER, current);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                glActiveTexture(GL_TEXTURE0 + 1);
                restoreTex[1] = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTextureMC(GL_TEXTURE_2D, texDepthBackup);
                glBindFramebuffer(GL_FRAMEBUFFER, current);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                glActiveTexture(GL_TEXTURE0 + 2);
                restoreTex[2] = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTextureMC(GL_TEXTURE_2D, texDepthWorking);
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                glActiveTexture(GL_TEXTURE0 + 3);
                restoreTex[3] = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTextureMC(GL_TEXTURE_2D, texDepthOverlay);
                glBindFramebuffer(GL_FRAMEBUFFER, overlay.framebufferObject);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

                // draw texture from main to working use program;
                glBindFramebuffer(GL_FRAMEBUFFER, working.framebufferObject);
                glUseProgram(program);

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glVertexAttribPointer(attrPos, 3, GL_FLOAT, false, 20, 0);
                glVertexAttribPointer(attrUV, 2, GL_FLOAT, false, 20, 12);
                glEnableVertexAttribArray(attrPos);
                glEnableVertexAttribArray(attrUV);

                prevDrawTex();
                glEnableMC(GL_DEPTH_TEST);
                glDisableMC(GL_BLEND);
                int depthFunc = glGetInteger(GL_DEPTH_FUNC);
                glDepthFuncMC(GL_ALWAYS);
                glDepthMaskMC(true);

                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

                glDepthMaskMC(false);
                glDepthFuncMC(depthFunc);
                glEnableMC(GL_BLEND);

                postDrawTex();

                glDisableVertexAttribArray(attrPos);
                glDisableVertexAttribArray(attrUV);
                glUseProgram(0);

                for (int i = 0; i < 4; i++) {
                    glActiveTexture(GL_TEXTURE0 + i);
                    glBindTextureMC(GL_TEXTURE_2D, restoreTex[i]);
                }
                glActiveTexture(GL_TEXTURE0);

                draw();

                glBlendFuncMC(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                drawTexture(overlay.framebufferTexture);
                glBlendFuncMC(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glDisableMC(GL_STENCIL_TEST);

                // restore stencil buffer
                blitFramebuffer(working.framebufferObject, current, width, height, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
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
        glDisableMC(GL_ALPHA_TEST);
        glDisableMC(GL_CULL_FACE);
        glDisableMC(GL_DEPTH_TEST);
        glDisableMC(GL_FOG);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
    }

    private void postDrawTex() {
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        glEnableMC(GL_FOG);
        glEnableMC(GL_DEPTH_TEST);
        glEnableMC(GL_CULL_FACE);
        glEnableMC(GL_ALPHA_TEST);
    }

    private void drawTexture(int textureNext) {

        prevDrawTex();

        int textureId = glGetInteger(GL_TEXTURE_BINDING_2D);
        if (textureNext >= 0)
            glBindTexture(GL_TEXTURE_2D, textureNext);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glVertexPointer(3, GL_FLOAT, 20, 0);
        glClientActiveTexture(GL_TEXTURE0);
        glTexCoordPointer(2, GL_FLOAT, 20, 12);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glDisableClientState(GL_VERTEX_ARRAY);
        glClientActiveTexture(GL_TEXTURE0);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);

        if (textureNext >= 0)
            glBindTextureMC(GL_TEXTURE_2D, textureId);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

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
            glDeleteTextures(texColorBackup);
        if (texDepthBackup >= 0)
            glDeleteTextures(texDepthBackup);
        if (texDepthWorking >= 0)
            glDeleteTextures(texDepthWorking);
        if (texDepthOverlay >= 0)
            glDeleteTextures(texDepthOverlay);
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

    private static void blitFramebuffer(int src, int tar, int width, int height, int masks) {
        int current = glGetInteger(GL_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);

        glBindFramebuffer(GL_FRAMEBUFFER, current);
    }

}
