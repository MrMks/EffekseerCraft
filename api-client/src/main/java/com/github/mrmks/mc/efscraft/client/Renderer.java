package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.common.PropertyFlags;
import com.github.mrmks.mc.efscraft.math.Matrix4f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

public abstract class Renderer {
    private EfsProgram program = null;
    private Thread createThread = null;

    private final RenderingQueue<?> queue;
    private long lastFrameTimer = -1;

    protected Renderer(RenderingQueue<?> queue) { this.queue = queue; }

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

    protected void update(long current, long divider, boolean isPaused,
                        Matrix4f matView, Vec3f vPos, Vec3f vPrev, float partial,
                        Matrix4f matProj)
    {
        EfsProgram program = getProgram();

        if (!isPaused) {
            program.setCameraMatrix(matView.copy().translatef(new Vec3f(vPrev).linearTo(vPos, partial).negative()).getFloats());
            program.setProjectionMatrix(matProj.getFloats());

            float frames;

            if (lastFrameTimer < 0) {
                frames = 1;
            } else {
                if (current < lastFrameTimer)
                    frames = 0;
                else
                    frames = Math.min((current - lastFrameTimer) * 60f / divider, 10);
            }

            if (PropertyFlags.ENABLE_CREATE_DEBUG_EFFECT)
                queue.createDebug();

            // strange enough, but we will not update when frames is 0
            if (frames > 0) {
                queue.update(frames, partial, program);
                program.update(frames);
            }
        }

        lastFrameTimer = current;
    }

    protected void updateAndRender(long current, long divider, boolean isPaused, Matrix4f matView, Vec3f vPos, Vec3f vPrev, float partial, Matrix4f matProj) {
        update(current, divider, isPaused, matView, vPos, vPrev, partial, matProj);
        draw();
    }

    protected void draw() {
        getProgram().draw();
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

}
