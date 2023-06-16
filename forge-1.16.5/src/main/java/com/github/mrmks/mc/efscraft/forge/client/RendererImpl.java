package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.FloatBuffer;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    @Override
    protected double[] getRenderViewEntityPos() {
        return viewPos;
    }

    @Override
    protected double[] getRenderViewEntityPrevPos() {
        return viewPos;
    }

    private Matrix4f camMat, projMat;
    private final double[] viewPos = new double[3];

    @Override
    protected void getModelviewMatrix(FloatBuffer buffer) {
        camMat.store(buffer);
    }

    @Override
    protected void getProjectionMatrix(FloatBuffer buffer) {
        projMat.store(buffer);
    }

    @SubscribeEvent
    public void onRenderEvent(RenderWorldLastEvent event) {
        if (Minecraft.useFancyGraphics()) return;

        this.camMat = event.getMatrixStack().last().pose();
        this.projMat = event.getProjectionMatrix();

        ActiveRenderInfo info = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d v3d = info.getPosition();
        viewPos[0] = v3d.x; viewPos[1] = v3d.y; viewPos[2] = v3d.z;

        updateAndRender(event.getPartialTicks(), event.getFinishTimeNano(), 1000_000_000, Minecraft.getInstance().isPaused());
    }

    private int lastFancy = -1;
    private final Drawer[] drawers = new Drawer[] {new DrawerFancy(), new DrawerPerfect()};

    @SubscribeEvent
    public void renderParticle(RenderParticleEvent event) {
        if (!Minecraft.useFancyGraphics()) return;

        this.camMat = event.cam;
        this.projMat = event.proj;

        Vector3d v3d = event.info.getPosition();
        viewPos[0] = v3d.x; viewPos[1] = v3d.y; viewPos[2] = v3d.z;

        if (event.prev)
            update(event.partial, event.nano, 1000_000_000, Minecraft.getInstance().isPaused());

        if (!apiSupport)
            return;

        int current = Minecraft.useShaderTransparency() ? 1 : 0;
        if (current != lastFancy) {
            if (lastFancy == 0 || lastFancy == 1)
                drawers[lastFancy].detach();
            lastFancy = current;
            drawers[current].attach();
        }
        drawers[current].drawEffect(event.prev);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
        if (world != null && world.isClientSide())
            unloadRender();
    }

    void cleanup() {
        for (Drawer drawer : drawers) drawer.cleanup();
        drawers[0] = drawers[1] = null;
    }

    public static class RenderParticleEvent extends Event {
        private final Matrix4f cam, proj;
        private final ActiveRenderInfo info;
        private final float partial;
        private final long nano;
        private final boolean prev;
        public RenderParticleEvent(float partial, long nano, Matrix4f cam, Matrix4f proj, ActiveRenderInfo info, boolean prev) {
            this.partial = partial;
            this.nano = nano;
            this.cam = cam;
            this.proj = proj;
            this.info = info;
            this.prev = prev;
        }
    }

    private interface Drawer {
        default void attach() {}
        void drawEffect(boolean prev);
        default void detach() {}
        void cleanup();
    }

    private class DrawerFancy implements Drawer {

        int workingFBO, colorAttach0, colorAttach1, depthAttach0;
        int backupFBO, depthAttach1;
        int colorTex0, colorTex1;
        int programHalfer = 0, programComp = 0, programPlain = 0;
        int vertexBuffer = -1;
        int lastWidth = -1, lastHeight = -1;

        DrawerFancy() {
            int vs, fs; String src;

            vs = glCreateShader(GL_VERTEX_SHADER);

            src = "#version 120\n" +
                    "\n" +
                    "attribute vec3 position;\n" +
                    "attribute vec2 uv;\n" +
                    "varying vec2 texCoord;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_Position = vec4(position, 1);\n" +
                    "    texCoord = uv;\n" +
                    "}\n";

            glShaderSource(vs, src);
            glCompileShader(vs);

            if (glGetShaderi(vs, GL_COMPILE_STATUS) == 0)
                System.out.println(glGetShaderInfoLog(vs));

            fs = glCreateShader(GL_FRAGMENT_SHADER);
            src = "#version 120\n" +
                    "\n" +
                    "uniform sampler2D texture0;\n" +
                    "varying vec2 texCoord;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec4 v = texture2D(texture0, texCoord);\n" +
                    "    v.a = v.a / 2;\n" +
                    "    gl_FragColor = v;\n" +
                    "}\n";

            glShaderSource(fs, src);
            glCompileShader(fs);

            if (glGetShaderi(fs, GL_COMPILE_STATUS) == 0)
                System.out.println(glGetShaderInfoLog(fs));

            int prog = glCreateProgram(), originProg = glGetInteger(GL_CURRENT_PROGRAM);

            glAttachShader(prog, vs);
            glAttachShader(prog, fs);
            glBindAttribLocation(prog, 0, "position");
            glBindAttribLocation(prog, 1, "uv");
            glLinkProgram(prog);

            if (glGetProgrami(prog, GL_LINK_STATUS) == 0)
                System.out.println(glGetProgramInfoLog(prog));

            glUseProgram(prog);
            glUniform1i(glGetUniformLocation(prog, "texture0"), 0);
            glUseProgram(originProg);

            programHalfer = prog;

            glDeleteShader(fs);

            fs = glCreateShader(GL_FRAGMENT_SHADER);
            src = "#version 120\n" +
                    "\n" +
                    "uniform sampler2D origin;\n" +
                    "uniform sampler2D current;\n" +
                    "varying vec2 texCoord;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec4 colorD0 = texture2D(origin, texCoord);\n" +
                    "    vec4 colorD1 = texture2D(current, texCoord);\n" +
                    "\n" +
                    "    float alpha0 = colorD0.a;\n" +
                    "    float alpha1 = colorD1.a;\n" +
                    "    float alphaS = (2 * alpha1 - alpha0) / (2 - alpha0);\n" +
                    "\n" +
                    "    colorD1.a = colorD1.a + alpha0 / 2 * (1 - alphaS);\n" +
                    "    gl_FragData[0] = colorD1;\n" +
                    "\n" +
                    "    vec3 colorS = colorD1.rgb - colorD0.rgb * (1 - alphaS);\n" +
                    "    colorS = colorS / alphaS;\n" +
                    "    gl_FragData[1] = vec4(colorS, alphaS);\n" +
                    "}\n";

            glShaderSource(fs, src);
            glCompileShader(fs);

            if (glGetShaderi(fs, GL_COMPILE_STATUS) == 0)
                System.out.println(glGetShaderInfoLog(fs));

            prog = glCreateProgram();
            glAttachShader(prog, vs);
            glAttachShader(prog, fs);
            glBindAttribLocation(prog, 0, "position");
            glBindAttribLocation(prog, 1, "uv");
            glLinkProgram(prog);

            if (glGetProgrami(prog, GL_LINK_STATUS) == 0)
                System.out.println(glGetProgramInfoLog(prog));

            glUseProgram(prog);
            glUniform1i(glGetUniformLocation(prog, "origin"), 0);
            glUniform1i(glGetUniformLocation(prog, "current"), 1);
            glUseProgram(originProg);

            programComp = prog;

            glDeleteShader(fs);

            fs = glCreateShader(GL_FRAGMENT_SHADER);
            src = "#version 120\n" +
                    "\n" +
                    "varying vec2 texCoord;\n" +
                    "uniform sampler2D texture0;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(texture0, texCoord);\n" +
                    "}\n";

            glShaderSource(fs, src);
            glCompileShader(fs);

            if (glGetShaderi(fs, GL_COMPILE_STATUS) == 0)
                System.out.println(glGetShaderInfoLog(fs));

            prog = glCreateProgram();
            glAttachShader(prog, vs);
            glAttachShader(prog, fs);
            glBindAttribLocation(prog, 0, "position");
            glBindAttribLocation(prog, 1, "uv");
            glLinkProgram(prog);

            if (glGetProgrami(prog, GL_LINK_STATUS) == 0)
                System.out.println(glGetProgramInfoLog(prog));

            glUseProgram(prog);
            glUniform1i(glGetUniformLocation(prog, "texture0"), 0);
            glUseProgram(originProg);

            programPlain = prog;

            glDeleteShader(vs);
            glDeleteShader(fs);

            float[] data = {
                    -1, -1, 0,    0, 0,
                    +1, -1, 0,    1, 0,
                    +1, +1, 0,    1, 1,
                    -1, +1, 0,    0, 1,
            };

            vertexBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        private void resize(int width, int height) {

            if (lastWidth == width && lastHeight == height)
                return;

            lastWidth = width;
            lastHeight = height;

            if (workingFBO <= 0) {
                workingFBO = glGenFramebuffers();
                backupFBO = glGenFramebuffers();

                int[] names = new int[4];
                glGenTextures(names);
                colorTex0 = names[0]; colorTex1 = names[1]; colorAttach0 = names[2]; colorAttach1 = names[3];
                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                for (int name : names) {
                    glBindTexture(GL_TEXTURE_2D, name);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                }
                glBindTexture(GL_TEXTURE_2D, originTex);

                names = new int[2];
                glGenRenderbuffers(names);
                depthAttach0 = names[0]; depthAttach1 = names[1];

                reallocateBuffers(width, height);

                int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttach0, 0);
                glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, colorAttach1, 0);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backupFBO);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);

                int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    System.out.println(status);
                }

                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
            } else {
                reallocateBuffers(width, height);
            }
        }

        private void reallocateBuffers(int width, int height) {
            int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
            for (int name : new int[] {colorTex0, colorTex1, colorAttach0, colorAttach1}) {
                glBindTexture(GL_TEXTURE_2D, name);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            }
            glBindTexture(GL_TEXTURE_2D, originTex);

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach0);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach1);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }

        private boolean[] setupCaps(int[] capsEnable, int[] capsDisable) {
            boolean[] cache = new boolean[capsEnable.length + capsDisable.length];
            int i = 0;
            for (int j = 0; j < capsEnable.length; i++, j++) {
                boolean flag = cache[i] = glIsEnabled(capsEnable[j]);
                if (!flag) glEnable(capsEnable[j]);
            }
            for (int j = 0; j < capsDisable.length; i++, j++) {
                boolean flag = cache[i] = glIsEnabled(capsDisable[j]);
                if (flag) glDisable(capsDisable[j]);
            }

            return cache;
        }

        private void restoreCaps(boolean[] flags, int[] capsEnable, int[] capsDisable) {
            int i = 0;
            for (int j = 0; j < capsEnable.length; i++, j++) {
                if (!flags[i]) glDisable(capsEnable[j]);
            }
            for (int j = 0; j < capsDisable.length; i++, j++)
                if (flags[i]) glEnable(capsDisable[j]);
        }

        private int[] setupTextures(int... names) {
            int[] cache = new int[names.length];
            int unit = glGetInteger(GL_ACTIVE_TEXTURE);
            for (int i = 0; i < names.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                cache[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, names[i]);
            }

            glActiveTexture(unit);
            return cache;
        }

        private void restoreTexture(int[] names) {
            int unit = glGetInteger(GL_ACTIVE_TEXTURE);
            for (int i = 0; i < names.length; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, names[i]);
            }

            glActiveTexture(unit);
        }

        private void drawRectangle(int program) {
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);

            glUseProgram(program);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
        }

        @Override
        public void drawEffect(boolean prev) {
            INT_16.clear();
            glGetIntegerv(GL_VIEWPORT, INT_16);

            int w, h;
            resize(w = INT_16.get(2), h = INT_16.get(3));

            int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

            if (prev) {
                // copy current screen to colorTex0;
                int bufferDraw = glGetInteger(GL_DRAW_BUFFER);
                if (originRead != originDraw)
                    glBindFramebuffer(GL_READ_FRAMEBUFFER, originDraw);

                int bufferRead = glGetInteger(GL_READ_BUFFER);
                if (bufferRead != bufferDraw)
                    glReadBuffer(bufferDraw);

                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, colorTex0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h); // to colorTex0
                glBindTexture(GL_TEXTURE_2D, originTex);

                if (bufferRead != bufferDraw)
                    glReadBuffer(bufferRead);

                // draw with half alpha;
                int originProgram = glGetInteger(GL_CURRENT_PROGRAM);
                int[] capsToEnable = {}, capsToDisable = {GL_DEPTH_TEST, GL_ALPHA_TEST, GL_BLEND, GL_LIGHTING, GL_FOG, GL_COLOR_MATERIAL};
                boolean[] originCaps = setupCaps(capsToEnable, capsToDisable);
                int[] originTexs = setupTextures(colorTex0);
                drawRectangle(programHalfer);

                // restore contexts
                glUseProgram(originProgram);
                restoreCaps(originCaps, capsToEnable, capsToDisable);
                restoreTexture(originTexs);

                // backup current depth to backupFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backupFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST); // to depthAttach0

                // bind framebuffers back;
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                if (originRead != originDraw)
                    glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);

                int error = glGetError();
                if (error != GL_NO_ERROR)
                    System.out.println(error);
            } else {
                // here we compute new colors to colorTex2 and revert what should the alpha be;
                int bufferDraw = glGetInteger(GL_DRAW_BUFFER);
                if (originRead != originDraw)
                    glBindFramebuffer(GL_READ_FRAMEBUFFER, originDraw);

                int bufferRead = glGetInteger(GL_READ_BUFFER);
                if (bufferRead != bufferDraw) glReadBuffer(bufferDraw);

                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, colorTex1);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h); // to colorTex1;
                glBindTexture(GL_TEXTURE_2D, originTex);

                if (bufferRead != bufferDraw) glReadBuffer(bufferRead);

                int originProgram = glGetInteger(GL_CURRENT_PROGRAM);
                int[] originTextures = setupTextures(0, 0, 0, 0);
                int[] capsEnable = {}, capsDisable = {GL_DEPTH_TEST, GL_ALPHA_TEST, GL_BLEND, GL_LIGHTING, GL_FOG, GL_COLOR_MATERIAL};
                boolean[] originCaps = setupCaps(capsEnable, capsDisable);

                // compute correct color;
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
                setupTextures(colorTex0, colorTex1);
                drawRectangle(programComp);
                restoreCaps(originCaps, capsEnable, capsDisable);

                // copy depth to workingFBO
                glDrawBuffer(GL_COLOR_ATTACHMENT0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw effects to workingFBO, and generate stencils
                capsEnable = new int[] {GL_STENCIL_TEST};capsDisable = new int[0];
                originCaps = setupCaps(capsEnable, capsDisable);
                glStencilOp(GL_KEEP, GL_REPLACE, GL_KEEP);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilMask(0xff);
                draw();
                restoreCaps(originCaps, capsEnable, capsDisable);

                // draw colorTex0 to screen
                capsDisable = new int[] {GL_DEPTH_TEST, GL_FOG, GL_LIGHTING, GL_COLOR_MATERIAL};
                originCaps = setupCaps(capsEnable, capsDisable);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glStencilFunc(GL_EQUAL, 1, 0xff);
                setupTextures(colorTex0);
                drawRectangle(programPlain);

                // copy depth to origin draw, this is the depth result we excepted.
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // copy depth from backupFBO
                glBindFramebuffer(GL_READ_FRAMEBUFFER, backupFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw effects
                draw();

                // draw translucent layer
                setupTextures(colorAttach1);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                drawRectangle(programPlain);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

                // copy workingFBO's first color attachment back to originDraw
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glReadBuffer(GL_COLOR_ATTACHMENT0);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);

                // restores
                restoreCaps(originCaps, capsEnable, capsDisable);
                restoreTexture(originTextures);
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                glUseProgram(originProgram);

                // check errors
                int error = glGetError();
                if (error != GL_NO_ERROR)
                    System.out.println(error);
            }
        }

        @Override
        public void cleanup() {

            glDeleteProgram(programHalfer);
            glDeleteProgram(programComp);
            glDeleteProgram(programPlain);

            glDeleteFramebuffers(workingFBO);
            glDeleteFramebuffers(backupFBO);

            int[] names = {colorTex0, colorTex1, colorAttach0, colorAttach1};
            glDeleteTextures(names);

            names = new int[] {depthAttach0, depthAttach1};
            glDeleteRenderbuffers(names);

            glDeleteBuffers(vertexBuffer);
        }
    }

    private class DrawerPerfect implements Drawer {

        @Override
        public void drawEffect(boolean prev) {

        }

        @Override
        public void cleanup() {

        }
    }
}
