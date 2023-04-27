package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EfsProgram;

class EfsProgramContainer {
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
        if (program != null && thread == Thread.currentThread())
            program.delete();
    }
}
