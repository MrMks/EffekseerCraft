package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class Renderer {
    private EfsProgram program = null;
    private Thread createThread = null;

    private final FloatBuffer buffer = ByteBuffer.allocateDirect(16 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final RenderingQueue queue;
    private long lastFrameTimer = -1;

    protected Renderer(RenderingQueue queue) {
        this.queue = queue;
    }

    private EfsProgram getProgram(float partialTicks) {
        synchronized (this) {
            if (program == null || createThread == null) {
                if (program != null) program.delete();

                program = new EfsProgram();
                program.initialize(8000);
                createThread = Thread.currentThread();
            } else {
                if (createThread != Thread.currentThread())
                    throw new IllegalStateException();
            }
        }

        float[] floats = new float[16], trans = new float[3];

        double[] prevPos = getRenderViewEntityPrevPos(), curPos = getRenderViewEntityPos();

        if (prevPos != null && curPos != null) {
            trans[0] += (prevPos[0] - curPos[0]) * partialTicks - prevPos[0];
            trans[1] += (prevPos[1] - curPos[1]) * partialTicks - prevPos[1];
            trans[2] += (prevPos[2] - curPos[2]) * partialTicks - prevPos[2];
        }

        getModelviewMatrix(buffer);
        buffer.get(floats).clear();
        for (int i = 0; i < 3; i++) {
            float t = 0;
            for (int j = 0; j < 3; j++) t += floats[i + 4 * j] * trans[j];
            floats[i + 12] += t;
        }
        program.setCameraMatrix(floats);

        // update projection matrix
        getProjectionMatrix(buffer);
        buffer.get(floats).clear();
        program.setProjectionMatrix(floats);

        return program;
    }

    protected void updateAndRender(float partial, long current, boolean isPaused) {

        EfsProgram program = getProgram(partial);

        if (!isPaused) {
            float frames;

            if (lastFrameTimer < 0) {
                frames = 1;
            } else {
                if (current < lastFrameTimer)
                    frames = 0;
                else
                    frames = Math.min((current - lastFrameTimer) * 60f / 1000, 10);
            }

            // strange enough, but we will not update when frames is 0
            if (frames > 0) {
                queue.update(frames, partial, program);
                program.update(partial);
            }
        }

        lastFrameTimer = current;

        program.draw();
    }

    protected void unloadRender() {
        queue.stopAll();
        deleteProgram();
    }

    public final void deleteProgram() {
        boolean flag = true;
        synchronized (this) {
            if (program != null) {
                flag = createThread == Thread.currentThread();

                if (flag) {
                    program.stopEffects();
                    program.delete();
                }

                program = null;
            }
            createThread = null;
        }

        if (!flag)
            throw new IllegalStateException();
    }

    protected abstract double[] getRenderViewEntityPos();
    protected abstract double[] getRenderViewEntityPrevPos();

    protected abstract void getModelviewMatrix(FloatBuffer buffer);
    protected abstract void getProjectionMatrix(FloatBuffer buffer);
}
