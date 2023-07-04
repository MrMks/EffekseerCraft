package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.*;

public class RendererImpl extends Renderer {
    protected RendererImpl(RenderingQueue queue) {
        super(queue);
    }

    @SubscribeEvent
    public void onRenderEvent(RenderWorldLastEvent event) {
        if (Minecraft.useFancyGraphics()) return;

        float[] floats = new float[16];
        FLOAT_16.clear();
        event.getMatrixStack().last().pose().store(FLOAT_16);
        FLOAT_16.get(floats);
        com.github.mrmks.mc.efscraft.math.Matrix4f matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

        FLOAT_16.clear();
        event.getProjectionMatrix().store(FLOAT_16);
        FLOAT_16.get(floats);
        com.github.mrmks.mc.efscraft.math.Matrix4f matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

        ActiveRenderInfo info = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d v3d = info.getPosition();
        Vec3f vPos = new Vec3f(v3d.x, v3d.y, v3d.z);

        Minecraft mc = Minecraft.getInstance();

        updateAndRender(event.getFinishTimeNano(), 1_000_000_000L, mc.isPaused(),
                matView, vPos, vPos, 0, matProj);
    }

    private int lastFancy = -1;
    private final Drawer[] drawers = new Drawer[] {new DrawerFancy(), new DrawerPerfect()};

    @SubscribeEvent
    public void renderParticle(RenderParticleEvent event) {
        if (!Minecraft.useFancyGraphics()) return;

        com.github.mrmks.mc.efscraft.math.Matrix4f matView, matProj;
        Vec3f vPos;

        {
            float[] floats = new float[16];
            FLOAT_16.clear();
            event.cam.store(FLOAT_16);
            FLOAT_16.get(floats);
            matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            FLOAT_16.clear();
            event.proj.store(FLOAT_16);
            FLOAT_16.get(floats);
            matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            Vector3d v3d = event.info.getPosition();
            vPos = new Vec3f(v3d.x, v3d.y, v3d.z);
        }

        if (event.prev)
            update(event.nano, 1000_000_000L, Minecraft.getInstance().isPaused(), matView, vPos, vPos, 0, matProj);

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

        int workingFBO, colorAttach0, depthAttach0;
        int backupFBO, depthAttach1;
        int colorTex0, colorTex1;
        int programPlain;
        int vertexBuffer;
        int lastWidth = -1, lastHeight = -1;

        DrawerFancy() {
            int vs, fs, prog; String src;
            int originProg = glGetInteger(GL_CURRENT_PROGRAM);

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

                int[] names = new int[3];
                glGenTextures(names);
                colorTex0 = names[0]; colorTex1 = names[1]; colorAttach0 = names[2];
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
            for (int name : new int[] {colorTex0, colorTex1, colorAttach0}) {
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
                if (bufferRead != bufferDraw) glReadBuffer(bufferDraw);

                int originTex = glGetInteger(GL_TEXTURE_BINDING_2D);
                glBindTexture(GL_TEXTURE_2D, colorTex0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h); // to colorTex0
                glBindTexture(GL_TEXTURE_2D, colorAttach0);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
                glBindTexture(GL_TEXTURE_2D, originTex);

                if (bufferRead != bufferDraw) glReadBuffer(bufferRead);

                FLOAT_16.clear();
                glGetFloatv(GL_COLOR_CLEAR_VALUE, FLOAT_16);
                glClearColor(0, 0, 0, 0);
                glClear(GL_COLOR_BUFFER_BIT);
                glClearColor(FLOAT_16.get(0), FLOAT_16.get(1), FLOAT_16.get(2), FLOAT_16.get(3));

                // backup current depth to backupFBO
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, backupFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST); // to depthAttach0

                // restores
                glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);

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
                int[] capsEnable = {}, capsDisable = {GL_DEPTH_TEST, GL_STENCIL_TEST, GL_ALPHA_TEST, GL_BLEND, GL_LIGHTING, GL_FOG, GL_COLOR_MATERIAL};
                boolean[] originCaps = setupCaps(capsEnable, capsDisable);

                // compute correct color;
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
//                glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
                glClear(GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
                setupTextures(colorTex1);
                glEnable(GL_BLEND);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                drawRectangle(programPlain);
                glDisable(GL_BLEND);
//                glDrawBuffer(GL_COLOR_ATTACHMENT0);

                // copy depth to workingFBO
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw effects to workingFBO, and generate stencils
                glEnable(GL_STENCIL_TEST);
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                glStencilFunc(GL_ALWAYS, 1, 0xff);
                glStencilMask(0xff);
                draw();

                // draw colorTex0 to workingFBO
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                glStencilFunc(GL_NOTEQUAL, 1, 0xff);
                setupTextures(colorTex0);
                drawRectangle(programPlain);

                // copy depth to origin drawing framebuffer, this is the depth result we excepted.
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // copy depth from backupFBO
                glBindFramebuffer(GL_READ_FRAMEBUFFER, backupFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, workingFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                // draw effects again
                draw();

                // draw translucent layer
                glEnable(GL_BLEND);
//                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                setupTextures(colorTex1);
                drawRectangle(programPlain);

                // copy workingFBO's first color attachment back to originDraw
                glBindFramebuffer(GL_READ_FRAMEBUFFER, workingFBO);
                glReadBuffer(GL_COLOR_ATTACHMENT0);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);

                // restores
                glDisable(GL_STENCIL_TEST);
                glDisable(GL_BLEND);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
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

            glDeleteProgram(programPlain);

            if (workingFBO > 0) {
                glDeleteFramebuffers(workingFBO);
                glDeleteFramebuffers(backupFBO);

                int[] names = {colorTex0, colorTex1, colorAttach0};
                glDeleteTextures(names);

                names = new int[]{depthAttach0, depthAttach1};
                glDeleteRenderbuffers(names);
            }

            glDeleteBuffers(vertexBuffer);
        }
    }

    private class DrawerPerfect implements Drawer {

        private int lastWidth = -1, lastHeight = -1;
        private int depthFBO = -1, depthAttach0, depthAttach1;

        private void resize(int w, int h) {

            if (lastWidth == w && lastHeight == h)
                return;

            lastWidth = w; lastHeight = h;

            if (depthFBO < 0) {
                depthFBO = glGenFramebuffers();

                int[] ints = new int[2];
                glGenRenderbuffers(ints);
                depthAttach0 = ints[0];
                depthAttach1 = ints[1];
            }

            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach0);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);
            glBindRenderbuffer(GL_RENDERBUFFER, depthAttach1);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, w, h);

            glBindRenderbuffer(GL_RENDERBUFFER, 0);
        }

        @Override
        public void drawEffect(boolean prev) {

            INT_16.clear();
            glGetIntegerv(GL_VIEWPORT, INT_16);
            int w = INT_16.get(2), h = INT_16.get(3);

            resize(w, h);

            int originRead = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            int originDraw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

            int mainFBO = Minecraft.getInstance().getMainRenderTarget().frameBufferId;

            if (prev) {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthFBO);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            } else {
                glBindFramebuffer(GL_READ_FRAMEBUFFER, mainFBO);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, depthFBO);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                glBindFramebuffer(GL_READ_FRAMEBUFFER, depthFBO);
                glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach0);
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, mainFBO);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);

                draw();

                glFramebufferRenderbuffer(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttach1);
                glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
            }

            glBindFramebuffer(GL_READ_FRAMEBUFFER, originRead);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, originDraw);
        }

        @Override
        public void cleanup() {
            if (depthFBO > 0) {
                glDeleteFramebuffers(depthFBO);
                glDeleteRenderbuffers(new int[]{depthAttach0, depthAttach1});
            }
        }
    }
}
