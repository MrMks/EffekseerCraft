package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EfsProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

class Renderer {

    public static boolean USE_GL_TRANSLATE = false;

    private final EfsProgramContainer container = new EfsProgramContainer();
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private final MessageQueue instQueue;
    private long lastFrameTimer = -1;

    Renderer(MessageQueue instQueue) {
        this.instQueue = instQueue;
    }

    private EfsProgram getProgram(float partialTicks) {

        EfsProgram program = container.getOrInitialize(8000);

        float[] floats = new float[16], trans = new float[3];

        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view != null) {
            trans[0] += (view.prevPosX - view.posX) * partialTicks - view.prevPosX;
            trans[1] += (view.prevPosY - view.posY) * partialTicks - view.prevPosY;
            trans[2] += (view.prevPosZ - view.posZ) * partialTicks - view.prevPosZ;
        }
        if (USE_GL_TRANSLATE) {
            int mode = glGetInteger(GL_MATRIX_MODE);

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glTranslated(trans[0], trans[1], trans[2]);

            glGetFloat(GL_MODELVIEW_MATRIX, buffer);
            glPopMatrix();
            glMatrixMode(mode);

            buffer.get(floats).clear();
        } else {
            glGetFloat(GL_MODELVIEW_MATRIX, buffer);
            buffer.get(floats).clear();
            for (int i = 0; i < 3; i++) {
                float t = 0;
                for (int j = 0; j < 3; j++) t += floats[i + 4 * j] * trans[j];
                floats[i + 12] += t;
            }
        }
        program.setCameraMatrix(floats);

        // update projection matrix
        glGetFloat(GL_PROJECTION_MATRIX, buffer);
        buffer.get(floats).clear();
        program.setProjectionMatrix(floats);

        return program;
    }

    private float count = -1;

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {

        float partial = event.getPartialTicks();

        EfsProgram program = getProgram(partial);

        long current = Minecraft.getSystemTime();

        if (!Minecraft.getMinecraft().isGamePaused()) {

            instQueue.update(partial, program);

            if (count < 0) {
                Entity entity = Minecraft.getMinecraft().player;
                instQueue.createDebug(program, entity.posX, entity.posY, entity.posZ);
                count = 300;
            }
            count -= partial;

            float frames;
            if (lastFrameTimer < 0) {
                frames = 1;
            } else {
                frames = current - lastFrameTimer;
                frames *= 60 / 1000f;
            }
            program.update(frames);
        }
        lastFrameTimer = current;

        program.draw();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        stopAll();
        container.delete();
    }

    private void stopAll() {
        EfsProgram program = container.get();
        if (program != null) {
            program.stopEffects();
        }

        instQueue.stopAll();
    }

    void deleteProgram() {
        container.delete();
    }

    static class EfsProgramContainer {
        private transient EfsProgram program;
        private transient Thread thread;

        synchronized EfsProgram getOrInitialize(int count) {
            if (create()) {
                program.initialize(count);
            }

            return program;
        }

        synchronized EfsProgram getOrInitialize(int count, boolean srgb) {
            if (create()) {
                program.initialize(count, srgb);
            }

            return program;
        }

        private synchronized boolean create() {
            if (program == null) {
                thread = Thread.currentThread();
                program = new EfsProgram();

                return true;
            } else if (thread != Thread.currentThread()) {
                throw new IllegalStateException();
            }

            return false;
        }

        synchronized EfsProgram get() {
            if (thread == null || thread == Thread.currentThread())
                return program;

            return null;
        }

        void delete() {
            if (program != null && thread == Thread.currentThread()) {
                program.delete();
                program = null;
                thread = null;
            }
        }
    }
}
