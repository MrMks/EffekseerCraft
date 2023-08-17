package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.common.PropertyFlags;
import com.github.mrmks.mc.efscraft.math.Matrix4f;

class EfsRenderer {

    private final EfsClient<?, ?, ?, ?> client;
    private final EfsDrawingQueue<?> queue;

    private EfsProgram program = null;
    private Thread createThread = null;
    private long lastFrameTimer = -1;

    EfsRenderer(EfsClient<?,?,?,?> client, EfsDrawingQueue<?> queue) {
        this.client = client;
        this.queue = queue;
    }

    void update(float partial, long nanoNow, boolean pausing, Matrix4f matProj, Matrix4f matModel) {
        EfsProgram program = getProgram();

        if (!pausing) {
            program.setCameraMatrix(matModel.getFloats());
            program.setProjectionMatrix(matProj.getFloats());

            float frames;

            if (lastFrameTimer < 0) {
                frames = 1;
            } else {
                if (nanoNow < lastFrameTimer)
                    frames = 0;
                else
                    frames = Math.min((nanoNow - lastFrameTimer) * 60f / 1_000_000_000, 10);
            }

            if (PropertyFlags.ENABLE_CREATE_DEBUG_EFFECT)
                queue.createDebug();

            if (frames > 0) {
                queue.update(frames, partial, program);
                program.update(frames);
            }
        }

        lastFrameTimer = nanoNow;
    }

    void draw() {
        getProgram().draw();
    }

    void stopAll() {
        queue.stopAll();
    }

    void clearAll() {
        stopAll();
        deleteProgram();
    }

    private EfsProgram getProgram() {
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

        return program;
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
}
