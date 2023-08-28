package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

class RendererImpl {

    private int workingFBO, colorAttachBuf, depthAttachBuf;
    private final int vertexBuffer;
    private final int programDepth;
    private final int texColor, texColorOverlay, texDepthBackup, texDepthWorking, texDepthOverlay;

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

    public void renderWorld(EfsRenderEvent event, EfsProgram program) {

        int w, h;

        INT_16.clear();
        glGetInteger(GL_VIEWPORT, INT_16);
        tryResize(w = INT_16.get(2), h = INT_16.get(3));

        if (openglSupported()) {
            int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
            if (event.getPhase() == EfsRenderEvent.Phase.START) {
                glBindTexture(GL_TEXTURE_2D, texDepthBackup);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            } else {
                glBindTexture(GL_TEXTURE_2D, texColor);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                glBindTexture(GL_TEXTURE_2D, texDepthWorking);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);

                int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);

                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texDepthBackup, 0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                program.setBackground(texColor, false);
                program.setDepth(texDepthWorking, false);
                program.draw();
                program.unsetBackground();
                program.unsetDepth();

                glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, texDepthWorking, 0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
            }
            glBindTexture(GL_TEXTURE_2D, originTex);
        } else {
            if (event.getPhase() == EfsRenderEvent.Phase.END) {
                program.draw();
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
