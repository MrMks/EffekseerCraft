package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.common.PropertyFlags;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

class RendererImpl {

    private int workingFBO, colorAttachBuf, depthAttachBuf;
    private final int vertexBuffer;
    private final int programDepth, programPlain;
    private final int texColor, texColorOverlay, texDepthBackup, texDepthWorking, texDepthOverlay;
    private int texColorOrigin;

    private int lastWidth = -1, lastHeight = -1;

    RendererImpl() {

        // bind vertex buffer
        float[] data = {
                -1f, -1f,  0,      0, 0,        // left-bottom
                +1f, -1f,  0,      1, 0,        // right-bottom
                +1f,  1f,  0,      1, 1,        // right-top
                -1f,  1f,  0,      0, 1,        // left-top
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

        // fsh to draw textures directly;
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

        // generate textures;
        INT_16.clear();
        INT_16.limit(5);
        glGenTextures(INT_16);
        texColor = INT_16.get(0);
        texDepthBackup = INT_16.get(1);
        texDepthOverlay = INT_16.get(2);
        texDepthWorking = INT_16.get(3);
        texColorOverlay = INT_16.get(4);

        int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);

        for (int i = 0; i < INT_16.limit(); i++) {
            glBindTexture(GL_TEXTURE_2D, INT_16.get(i));
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }

        glBindTexture(GL_TEXTURE_2D, originTex);
    }

    static float[] getModelviewMatrix() {
        FLOAT_16.clear();
        glGetFloat(GL_MODELVIEW_MATRIX, FLOAT_16);
        float[] floats = new float[16];
        FLOAT_16.get(floats);

        return floats;
    }

    static float[] getProjectionMatrix() {
        FLOAT_16.clear();
        glGetFloat(GL_PROJECTION_MATRIX, FLOAT_16);
        float[] floats = new float[16];
        FLOAT_16.get(floats);

        return floats;
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

            int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);

            glBindTexture(GL_TEXTURE_2D, texColor);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, w, h, 0, GL_RGBA, GL_FLOAT, 0);
            glBindTexture(GL_TEXTURE_2D, texDepthBackup);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 0);
            glBindTexture(GL_TEXTURE_2D, texDepthWorking);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 0);
            glBindTexture(GL_TEXTURE_2D, texDepthOverlay);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 0);

            glBindTexture(GL_TEXTURE_2D, texColorOverlay);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, w, h, 0, GL_RGBA, GL_FLOAT, 0);

            glBindTexture(GL_TEXTURE_2D, originTex);

            if (workingFBO <= 0) {
                colorAttachBuf = glGenRenderbuffers();
                depthAttachBuf = glGenRenderbuffers();
            }

            glBindRenderbuffer(GL_RENDERBUFFER, colorAttachBuf);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA16F, w, h);
            glBindRenderbuffer(GL_RENDERBUFFER, depthAttachBuf);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);

            if (workingFBO <= 0) {
                workingFBO = glGenFramebuffers();
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                if (PropertyFlags.ENABLE_TRANSPARENCY) {
                    glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorAttachBuf);
                    glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttachBuf);
                    glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthAttachBuf);
                }

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentDraw);
            }

            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentDraw);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, currentRead);

            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.out.println("error after efscraft resized: " + error );
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

    public void renderWorld(EfsRenderEvent event, Runnable drawer) {

        int w, h;

        INT_16.clear();
        glGetInteger(GL_VIEWPORT, INT_16);
        tryResize(w = INT_16.get(2), h = INT_16.get(3));

        Minecraft mc = Minecraft.getMinecraft();

        if (PropertyFlags.ENABLE_TRANSPARENCY && openglSupported())
        {
            // record current states
            int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

            int[] caps = {GL_DEPTH_TEST, GL_ALPHA_TEST, GL_STENCIL_TEST, GL_BLEND, GL_FOG, GL_LIGHTING, GL_COLOR_MATERIAL};
            boolean[] originCaps = new boolean[caps.length];
            for (int i = 0; i < caps.length; i++) {
                originCaps[i] = glIsEnabled(caps[i]);
                if (originCaps[i]) glDisable(caps[i]);
            }
            int originUnit = glGetInteger(GL_ACTIVE_TEXTURE);
            int[] originTextures = new int[4];
            for (int i = 0; i < originTextures.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                originTextures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            }
            glActiveTexture(originUnit);

            if (event instanceof EfsRenderEvent.Prev)
            {
                // clear working and backup;
                float[] cls = new float[4];
                FLOAT_16.clear();
                glGetFloat(GL_COLOR_CLEAR_VALUE, FLOAT_16);
                FLOAT_16.get(cls);
                glDepthMask(true);
                glClearColor(0, 0, 0, 0);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

                // backup color on COLOR_ATTACHMENT0 to working
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originDraw);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                int rb = glGetInteger(GL_READ_BUFFER);
                glReadBuffer(glGetInteger(GL_DRAW_BUFFER));
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                glReadBuffer(rb);

                // copy depth to texDepthBackup
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, texDepthBackup);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // bind origin framebuffers back
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);

                // backup drawing buffers;
                int[] draws = backupDrawBuffers();
                glDrawBuffer(GL_COLOR_ATTACHMENT0);

                // bind texColor to color attachment 0 and clear color
                texColorOrigin = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texColor, 0);
                glClear(GL_COLOR_BUFFER_BIT);
                glClearColor(cls[0], cls[1], cls[2], cls[3]);

                // restore drawing buffers;
                restoreDrawBuffers(draws);

                // setup states
                glDepthMask(false);
                glDepthMask(true);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

                int error = glGetError();
                if (error != GL_NO_ERROR)
                    System.out.println(error);
            }
            else
            {
                // restore states
                glDepthMask(false);

                // backup origin program;
                int originProgram = glGetInteger(GL_CURRENT_PROGRAM);
                glUseProgram(0);

                // setup textures
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, texColorOverlay);
                glActiveTexture(GL_TEXTURE0 + 1);
                glBindTexture(GL_TEXTURE_2D, texDepthBackup);
                glActiveTexture(GL_TEXTURE0 + 2);
                glBindTexture(GL_TEXTURE_2D, texDepthWorking);
                glActiveTexture(GL_TEXTURE0 + 3);
                glBindTexture(GL_TEXTURE_2D, texDepthOverlay);

                // copy color to texColorOverlay
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originDraw);
                int rb = glGetInteger(GL_READ_BUFFER);
                glReadBuffer(glGetInteger(GL_DRAW_BUFFER));
                glActiveTexture(GL_TEXTURE0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                glReadBuffer(rb);
                glBindTexture(GL_TEXTURE_2D, texColor);

                // bind origin color attachment 0
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texColorOrigin, 0);
                texColorOrigin = -1;

                // copy depth to workingFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // copy depth to texDepthOverlay
                glActiveTexture(GL_TEXTURE0 + 3);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // copy working's color to texColorBackup
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glActiveTexture(GL_TEXTURE0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // draw backup's color to working;
                glEnable(GL_BLEND);
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                glBindTexture(GL_TEXTURE_2D, texColorOverlay);
                drawRectangle(programPlain);
                glDisable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glBindTexture(GL_TEXTURE_2D, texColor);

                // draw effect and generate stencils
                glEnable(GL_STENCIL_TEST);
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilMask(0xff);
//                update(event.partial, event.finishNano, 1_000_000_000L, Minecraft.getMinecraft().isGamePaused());
                {
                    Entity entity = mc.getRenderViewEntity();
                    Vec3f vPos = entity == null ? new Vec3f() : new Vec3f(entity.posX, entity.posY, entity.posZ);
                    Vec3f vPrev = entity == null ? new Vec3f() : new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
//                    updateAndRender(event.finishNano, 1_000_000_000L, mc.isGamePaused(),
//                            new Matrix4f(getModelviewMatrix()), vPos, vPrev, event.partial,
//                            new Matrix4f(getProjectionMatrix()));
                }
                drawer.run();

                // copy current working's depth to texDepthWorking
                glActiveTexture(GL_TEXTURE0 + 2);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                // draw color back
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                drawRectangle(programPlain);

                // draw depth back
                glEnable(GL_BLEND);
                glDisable(GL_STENCIL_TEST);
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_ALWAYS);
                glDepthMask(true);
                drawRectangle(programDepth);
                glDisable(GL_BLEND);
                glEnable(GL_STENCIL_TEST);
                glDisable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
                glDepthMask(false);

                // draw effect again in stencils
                drawer.run();

                // draw translucent layer again
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, texColorOverlay);
                glEnable(GL_BLEND);
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                drawRectangle(programPlain);
                glBindTexture(GL_TEXTURE_2D, texColor);
                glDisable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                // we got every thing we need, copy them to originDraw
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
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
                    glEnable(caps[i]);
                else
                    glDisable(caps[i]);
            }
            for (int i = 0; i < originTextures.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, originTextures[i]);
            }
            glActiveTexture(originUnit);
        }
        else
        {
            if (openglSupported())
            {
                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                if (event instanceof EfsRenderEvent.Prev)
                {
                    glBindTexture(GL_TEXTURE_2D, texDepthBackup);
                    glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                }
                else
                {
                    glBindTexture(GL_TEXTURE_2D, texDepthWorking);
                    glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                    int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);

                    glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                    glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texDepthBackup, 0);
                    glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                    drawer.run();

//                    Entity entity = mc.getRenderViewEntity();
//                    Vec3f vPos = entity == null ? new Vec3f() : new Vec3f(entity.posX, entity.posY, entity.posZ);
//                    Vec3f vPrev = entity == null ? new Vec3f() : new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
//                    updateAndRender(event.finishNano, 1000_000_000L, mc.isGamePaused(),
//                            new Matrix4f(getModelviewMatrix()), vPos, vPrev, event.partial,
//                            new Matrix4f(getProjectionMatrix())
//                    );

                    glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texDepthWorking, 0);
                    glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                    glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                }
                glBindTexture(GL_TEXTURE_2D, originTex);
            }
            else
            {
                if (event instanceof EfsRenderEvent.Post) {
//                    Entity entity = mc.getRenderViewEntity();
//                    Vec3f vPos = entity == null ? new Vec3f() : new Vec3f(entity.posX, entity.posY, entity.posZ);
//                    Vec3f vPrev = entity == null ? new Vec3f() : new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);

//                    updateAndRender(event.finishNano, 1000_000_000L, mc.isGamePaused(),
//                            new Matrix4f(getModelviewMatrix()), vPos, vPrev, event.partial,
//                            new Matrix4f(getProjectionMatrix())
//                    );
                    drawer.run();
                }
            }
        }
    }

//    @SubscribeEvent
//    public void onWorldUnload(WorldEvent.Unload event) {
//        World world = event.getWorld();
//        if (world != null && world.isRemote) unloadRender();
//    }

    public void deleteFramebuffer() {
        if (vertexBuffer >= 0)
            glDeleteBuffers(vertexBuffer);
        if (programDepth >= 0)
            glDeleteProgram(programDepth);
        if (texColor >= 0)
            glDeleteTextures(texColor);
        if (texDepthBackup >= 0)
            glDeleteTextures(texDepthBackup);
        if (texDepthWorking >= 0)
            glDeleteTextures(texDepthWorking);
        if (texDepthOverlay >= 0)
            glDeleteTextures(texDepthOverlay);
    }

    public static class RenderParticleEvent extends Event {
        final int pass;
        final float partial;
        final long finishNano;
        final boolean prev;

        RenderParticleEvent(int pass, float partial, long finishNano, boolean prev) {
            this.pass = pass;
            this.partial = partial;
            this.finishNano = finishNano;
            this.prev = prev;
        }
    }

}
