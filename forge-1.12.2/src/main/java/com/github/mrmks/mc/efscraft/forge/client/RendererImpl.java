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

    private Framebuffer working = null, backup = null;
    private final int vertexBuffer;
    private final int programDepth, programAlpha, programPlain;
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

        vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        int vs, fs, prog; String src;
        int originProg = glGetInteger(GL_CURRENT_PROGRAM);

        // common vertex shader
        src = "#version 120\n" +
                "attribute vec3 Position;\n" +
                "attribute vec2 UV;\n" +
                "varying vec2 texCoord;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(Position, 1);\n" +
                "    texCoord = UV;\n" +
                "}\n";

        vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, src);
        glCompileShader(vs);

        if (glGetShaderi(vs, GL_COMPILE_STATUS) == GL_FALSE)
            System.out.println(glGetShaderInfoLog(vs));

        // fsh to compute depth;
        src = "#version 120\n" +
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
                "    gl_FragColor = vec4(0);\n" +
                "    if (d1 < d2) {\n" +
                "        gl_FragDepth = d1;\n" +
                "    } else {\n" +
                "        gl_FragDepth = d0;\n" +
                "    }\n" +
                "}\n";

        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, src);
        glCompileShader(fs);

        if (glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE)
            System.out.println(glGetShaderInfoLog(fs));

        prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glBindAttribLocation(prog, 0, "Position");
        glBindAttribLocation(prog, 1, "UV");
        glLinkProgram(prog);

        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(prog));
        }

        glUseProgram(prog);
        glUniform1i(glGetUniformLocation(prog, "backupColor"), 0);
        glUniform1i(glGetUniformLocation(prog, "backupDepth"), 1);
        glUniform1i(glGetUniformLocation(prog, "workingDepth"), 2);
        glUniform1i(glGetUniformLocation(prog, "overlayDepth"), 3);

        glDeleteShader(fs);

        programDepth = prog;

        // fsh to compute correct alpha;
        src = "#version 120\n" +
                "\n" +
                "varying vec2 texCoord;\n" +
                "uniform sampler2D tex;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 tmp = texture2D(tex, texCoord);\n" +
                "    float alpha = inversesqrt(tmp.a);\n" +
                "    gl_FragColor = tmp * alpha;\n" +
                "}\n";
        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, src);
        glCompileShader(fs);

        if (glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE)
            System.out.println(glGetShaderInfoLog(fs));

        prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glBindAttribLocation(prog, 0, "Position");
        glBindAttribLocation(prog, 1, "UV");
        glLinkProgram(prog);

        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(prog));
        }

        glUseProgram(prog);
        glUniform1i(glGetUniformLocation(prog, "tex"), 0);

        glDeleteShader(fs);

        programAlpha = prog;

        // fsh draw color with no change;
        src = "#version 120\n" +
                "\n" +
                "varying vec2 texCoord;\n" +
                "uniform sampler2D tex;\n" +
                "\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(tex, texCoord);\n" +
                "}\n";
        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, src);
        glCompileShader(fs);

        if (glGetShaderi(fs, GL_COMPILE_STATUS) == GL_FALSE)
            System.out.println(glGetShaderInfoLog(fs));

        prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glBindAttribLocation(prog, 0, "Position");
        glBindAttribLocation(prog, 1, "UV");
        glLinkProgram(prog);

        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(prog));
        }

        glUseProgram(prog);
        glUniform1i(glGetUniformLocation(prog, "tex"), 0);

        glDeleteShader(fs);

        programPlain = prog;

        glDeleteShader(vs);
        glUseProgram(originProg);

        // generate textures to use;
        texColorBackup = glGenTextures();
        texDepthBackup = glGenTextures();
        texDepthOverlay = glGenTextures();
        texDepthWorking = glGenTextures();

        int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);

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

        glBindTextureMC(GL_TEXTURE_2D, originTex);
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

            int currentDraw, currentRead;

            currentDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
            currentRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);

            if (working == null) {
                working = new Framebuffer(w, h, true);
                working.enableStencil();
                working.setFramebufferColor(0, 0, 0, 0);
            } else {
                working.deleteFramebuffer();
                working.createFramebuffer(w, h);
            }

            if (backup == null) {
                backup = new Framebuffer(w, h, true);
                backup.setFramebufferColor(0, 0, 0, 0);
            } else {
                backup.deleteFramebuffer();
                backup.createFramebuffer(w, h);
            }

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentDraw);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, currentRead);

            currentDraw = glGetInteger(GL_TEXTURE_BINDING_2D);

            glBindTextureMC(GL_TEXTURE_2D, texColorBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthWorking);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);
            glBindTextureMC(GL_TEXTURE_2D, texDepthOverlay);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (ByteBuffer) null);

            glBindTextureMC(GL_TEXTURE_2D, currentDraw);

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.out.println("error after efscraft resize: " + error );
            }
        }
    }

    private int[] backupDrawBuffers() {
        int[] cache = new int[5];
        for (int i = 0; i < cache.length - 1; i++)
            cache[i] = glGetInteger(GL_DRAW_BUFFER0 + i);
        cache[cache.length - 1] = glGetInteger(GL_DRAW_BUFFER);
        return cache;
    }

    private void restoreDrawBuffers(int[] cache) {
        if (cache.length < 1) return;
        glDrawBuffer(cache[cache.length - 1]);
        INT_16.clear();
        INT_16.put(cache, 0, cache.length - 1);
        INT_16.limit(INT_16.position());
        INT_16.position(0);
        glDrawBuffers(INT_16);
    }

    private void drawRectangle(int program) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glUseProgram(program);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @SubscribeEvent
    public void renderWorld(RenderParticleEvent event) {

        int w, h;

        INT_16.clear();
        glGetInteger(GL_VIEWPORT, INT_16);
        tryResize(w = INT_16.get(2), h = INT_16.get(3));

        if (translucent && openglSupported())
        {
            // record current states
            int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

            int[] caps = {GL_DEPTH_TEST, GL_ALPHA_TEST, GL_STENCIL_TEST, GL_BLEND, GL_FOG, GL_LIGHTING, GL_CULL_FACE, GL_COLOR_MATERIAL};
            boolean[] originCaps = new boolean[caps.length];
            for (int i = 0; i < caps.length; i++) {
                originCaps[i] = glIsEnabled(caps[i]);
                if (originCaps[i]) glDisableMC(caps[i]);
            }
            int originUnit = glGetInteger(GL_ACTIVE_TEXTURE);
            int[] originTextures = new int[4];
            for (int i = 0; i < originTextures.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                originTextures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            }
            glActiveTexture(originUnit);

            if (event.prev)
            {
                // clear working and overlay;
                float[] cls = new float[4];
                FLOAT_16.clear();
                glGetFloat(GL_COLOR_CLEAR_VALUE, FLOAT_16);
                FLOAT_16.get(cls);
                glDepthMaskMC(true);
                glClearColor(0, 0, 0, 0);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, working.framebufferObject);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backup.framebufferObject);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                // backup color on COLOR_ATTACHMENT0 to working
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originDraw);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, working.framebufferObject);
                int rb = glGetInteger(GL_READ_BUFFER);
                glReadBuffer(GL_COLOR_ATTACHMENT0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                glReadBuffer(rb);

                // backup depth to backupFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backup.framebufferObject);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // bind origin framebuffers back
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);

                // backup drawing buffers;
                int[] draws = backupDrawBuffers();

                // clear color_attachment0
                glDrawBuffer(GL_COLOR_ATTACHMENT0);
                glClear(GL_COLOR_BUFFER_BIT);
                glClearColor(cls[0], cls[1], cls[2], cls[3]);

                // restore drawing buffers;
                restoreDrawBuffers(draws);

                // setup states
                glDepthMaskMC(true);

                int error = glGetError();
                if (error != GL_NO_ERROR)
                    System.out.println(error);
            }
            else
            {
                // restore states
                glDepthMaskMC(false);

                // backup origin program;
                int originProgram = glGetInteger(GL_CURRENT_PROGRAM);
                glUseProgram(0);

                // setup textures
                glActiveTexture(GL_TEXTURE0);
                glBindTextureMC(GL_TEXTURE_2D, texColorBackup);
                glActiveTexture(GL_TEXTURE0 + 1);
                glBindTextureMC(GL_TEXTURE_2D, texDepthBackup);
                glActiveTexture(GL_TEXTURE0 + 2);
                glBindTextureMC(GL_TEXTURE_2D, texDepthWorking);
                glActiveTexture(GL_TEXTURE0 + 3);
                glBindTextureMC(GL_TEXTURE_2D, texDepthOverlay);

                // copy color to texColorBackup
                int readBuffer = glGetInteger(GL_READ_BUFFER);
                glReadBuffer(GL_COLOR_ATTACHMENT0);
                glActiveTexture(GL_TEXTURE0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                glReadBuffer(readBuffer);

                // copy depth to texDepthOverlay
                glActiveTexture(GL_TEXTURE0 + 3);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // copy depth to workingFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, working.framebufferObject);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw correct color to backupFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backup.framebufferObject);
                glActiveTexture(GL_TEXTURE0);
                drawRectangle(programAlpha);

                // copy working's color to texColorBackup
                glBindFramebuffer(GL_READ_FRAMEBUFFER, working.framebufferObject);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // draw backup color to working;
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, working.framebufferObject);
                glEnableMC(GL_BLEND);
                glBindTextureMC(GL_TEXTURE_2D, backup.framebufferTexture);
                drawRectangle(programPlain);
                glDisableMC(GL_BLEND);
                glBindTextureMC(GL_TEXTURE_2D, texColorBackup);

                // draw effect and generate stencils
                glEnableMC(GL_STENCIL_TEST);
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilMask(0xff);
                update(event.partial, event.finishNano, 1_000_000_000L, Minecraft.getMinecraft().isGamePaused());
                draw();

                // copy current working's depth to texDepthWorking
                glActiveTexture(GL_TEXTURE0 + 2);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // copy backup's depth to texDepthBackup
                glBindFramebuffer(GL_READ_FRAMEBUFFER, backup.framebufferObject);
                glActiveTexture(GL_TEXTURE0 + 1);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // draw color back
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                glEnableMC(GL_BLEND);
                drawRectangle(programPlain);

                // draw depth back
                glDisableMC(GL_STENCIL_TEST);
                glEnableMC(GL_DEPTH_TEST);
                glDepthFuncMC(GL_ALWAYS);
                glDepthMaskMC(true);
                drawRectangle(programDepth);
                glDisableMC(GL_BLEND);
                glEnableMC(GL_STENCIL_TEST);
                glDisableMC(GL_DEPTH_TEST);
                glDepthFuncMC(GL_LEQUAL);
                glDepthMaskMC(false);

                // draw effect again in stencils
                draw();

                // draw translucent layer again
                glActiveTexture(GL_TEXTURE0);
                glBindTextureMC(GL_TEXTURE_2D, backup.framebufferTexture);
                glEnableMC(GL_BLEND);
                drawRectangle(programPlain);
                glDisableMC(GL_BLEND);

                // we got every thing we need, copy them to originDraw
                glBindFramebuffer(GL_READ_FRAMEBUFFER, working.framebufferObject);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                int[] draws = backupDrawBuffers();
                glDrawBuffer(GL_COLOR_ATTACHMENT0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, GL_NEAREST);
                restoreDrawBuffers(draws);

                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                glUseProgram(originProgram);

                int error = glGetError();
                if (error != GL_NO_ERROR)
                    System.out.println(error);
            }

            // restore to recorded states
            for (int i = 0; i < caps.length; i++) {
                if (originCaps[i])
                    glEnableMC(caps[i]);
                else
                    glDisableMC(caps[i]);
            }
            for (int i = 0; i < originTextures.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTextureMC(GL_TEXTURE_2D, originTextures[i]);
            }
            glActiveTexture(originUnit);
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

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
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
        if (backup != null)
            backup.deleteFramebuffer();
        if (working != null)
            working.deleteFramebuffer();
        if (vertexBuffer >= 0)
            glDeleteBuffers(vertexBuffer);
        if (programDepth >= 0)
            glDeleteProgram(programDepth);
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
        int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, src);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, tar);

        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, masks, GL_NEAREST);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
    }

}
