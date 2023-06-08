package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.common.packet.SPacketStop;
import com.github.mrmks.mc.efscraft.common.packet.SPacketTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class RenderingQueue {

    private final Queue<Entry> present = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<String, Entry>> lookup = new ConcurrentHashMap<>();
    private final AtomicBoolean clearMark = new AtomicBoolean(false);
    private final Queue<SPacketTrigger> triggers = new ConcurrentLinkedQueue<>();

    private final Function<String, EfsEffect> effects;
    private final EntityConvert convert;

    public RenderingQueue(Function<String, EfsEffect> getter, EntityConvert convert) {
        this.effects = getter;
        this.convert = convert;
    }

    private void putEntry(Entry entry, boolean overwrite) {

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.key, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (overwrite || old == null) {
            present.add(entry);
            submap.put(entry.emitter, entry);

            if (old != null)
                old.state = State.STOPPING_O;
        }
    }

    PlayBuilder commandPlay(String key, String effect, String emitter, int lifespan, int skip, Matrix4f local,
                     float[] modelPos, float[] modelRot, float[] dynamic, boolean overwrite)
    {
        Entry entry = new Entry(key, effect, emitter, lifespan, skip, local, modelPos, modelRot, dynamic);

        return new PlayBuilder(entry, overwrite);
    }

    void commandStop(SPacketStop stop) {
        if (clearMark.get()) return;

        String key = stop.getKey(), emitter = stop.getEmitter();

        Map<String, Entry> lookup = this.lookup.get(key);
        if (lookup != null) {
            if (emitter.isEmpty() || "*".equals(emitter)) {
                lookup.values().forEach(it -> it.state = State.STOPPING);
            } else {
                Entry entry = lookup.get(emitter);
                if (entry != null) {
                    entry.state = State.STOPPING;
                }
            }
        }
    }

    void commandTrigger(SPacketTrigger trigger) {
        if (clearMark.get()) return;

        triggers.add(trigger);
    }

    void commandClear() {
        clearMark.set(true);
    }

    void update(float frameGap, float partial, EfsProgram program) {
        // process pending commands;
        if (clearMark.get()) {
            present.clear();
            lookup.clear();

            program.stopEffects();
            clearMark.set(false);
        } else {
            present.removeIf(entry -> {
                if (entry.state == State.NEW) {
                    EfsEffect effect = effects.apply(entry.effect);
                    if (effect == null) {
                        entry.state = State.STOPPED;
                    } else {
                        entry.init(program.playEffect(effect));
                    }
                }

                if (entry.state == State.CREATED || entry.state == State.RUNNING) {
                    entry.update(frameGap, partial);
                }

                if (entry.state == State.STOPPING || entry.state == State.STOPPING_O) {
                    entry.stop();
                }

                if (entry.state == State.STOPPED) {
                    lookup.getOrDefault(entry.key, Collections.emptyMap()).remove(entry.emitter);
                    return true;
                } else return entry.state == State.STOPPED_O;
            });

            triggers.forEach(trigger -> {
                String key = trigger.getKey(), emitter = trigger.getEmitter();
                int id = trigger.getTrigger();
                Map<String, Entry> lookup = this.lookup.get(key);
                if (lookup != null) {
                    if (emitter.isEmpty() || "*".equals(emitter)) {
                        lookup.values().forEach(it -> it.handle.sendTrigger(id));
                    } else {
                        Entry entry = lookup.get(emitter);
                        if (entry != null)
                            entry.handle.sendTrigger(id);
                    }
                }
            });
            triggers.clear();
        }
    }

    void stopAll() {
        present.clear();
        lookup.clear();

        clearMark.set(true);
    }

    void createDebug() {
        Matrix4f local = new Matrix4f().identity();

        Entry entry = new Entry("laser03", "laser03", "debug", 203, 0, local, new float[3], new float[2], null);
        Controller controller = new ControllerAt(new float[] {0, 10, 0}, new float[2]);

        entry.setController(controller);

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.key, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (old != null) old.state = State.STOPPING;

        submap.put(entry.emitter, entry);
        present.add(entry);
    }

    class PlayBuilder {
        private final Entry entry;
        private final boolean overwrite;
        private boolean mark = false;

        private PlayBuilder(Entry entry, boolean overwrite) {
            this.entry = entry;
            this.overwrite = overwrite;
        }

        void playAt(float[] initPos, float[] initRot) {
            if (mark) return;
            mark = true;

            if (clearMark.get()) return;
            ControllerAt controller = new ControllerAt(initPos, initRot);
            entry.setController(controller);
            putEntry(entry, overwrite);
        }

        void playWith(int target, boolean followX, boolean followY, boolean followZ, boolean followYaw, boolean followPitch,
                      boolean inheritYaw, boolean inheritPitch, boolean useHead, boolean useRender) {
            if (mark) return;
            mark = true;

            if (clearMark.get()) return;
            ControllerWith controller = new ControllerWith(convert, target,
                    followX, followY, followZ, followYaw, followPitch,
                    inheritYaw, inheritPitch, useHead, useRender);
            entry.setController(controller);
            putEntry(entry, overwrite);
        }
    }

    enum State { NEW, CREATED, RUNNING, STOPPING, STOPPING_O, STOPPED, STOPPED_O }

    private static class Entry {

        final String key, effect, emitter;
        private final int skipFrame, lifespan;
        private final Matrix4f local;
        protected final float[] rotModel, posModel;
        private final float[] dynamics;

        private Controller controller;

        private EfsEffectHandle handle;
        private float lifeLength;
        transient RenderingQueue.State state;
        Entry(String key, String effect, String emitter, int lifespan, int skip, Matrix4f local, float[] posModel, float[] rotModel, float[] dynamics) {
            this.key = key;
            this.effect = effect;
            this.emitter = emitter;
            this.skipFrame = skip;
            this.lifespan = lifespan;
            this.dynamics = dynamics;

            this.local = local;
            this.rotModel = rotModel;
            this.posModel = posModel;

            this.lifeLength = 0;
            this.handle = null;
            state = RenderingQueue.State.NEW;
        }

        void setController(Controller controller) {
            this.controller = controller;
        }

        void init(EfsEffectHandle handle) {
            this.handle = handle;
            handle.setProgress(skipFrame);
            if (dynamics != null) {
                for (int i = 0; i < dynamics.length; i++)
                    handle.setDynamicInput(i, dynamics[i]);
            }
            state = RenderingQueue.State.CREATED;
        }

        void update(float frames, float partial) {

            if (lifeLength >= lifespan || !handle.exists() || !controller.isAlive())
                state = RenderingQueue.State.STOPPING;
            else {
                lifeLength += frames;
                if (state == RenderingQueue.State.CREATED) {

                    Matrix4f matrix4f = controller.initMatrix(partial, posModel[0], posModel[1], posModel[2], rotModel[0], rotModel[1]).mul(local);
                    handle.setBaseTransformMatrix(matrix4f.getFloats());

                    state = RenderingQueue.State.RUNNING;
                } else {
                    Matrix4f matrix4f = controller.updateMatrix(partial, posModel[0], posModel[1], posModel[2], rotModel[0], rotModel[1]);

                    if (matrix4f != null) {
                        handle.setBaseTransformMatrix(matrix4f.mul(local).getFloats());
                    }
                }
            }

        }

        void stop() {
            if (handle != null)
                handle.stop();
            if (state == State.STOPPING)
                state = State.STOPPED;
            else if (state == State.STOPPING_O)
                state = State.STOPPED_O;
        }
    }

    private abstract static class Controller {
        @Nonnull
        abstract Matrix4f initMatrix(float partial, float x, float y, float z, float yaw, float pitch);

        @Nullable
        abstract Matrix4f updateMatrix(float partial, float x, float y, float z, float yaw, float pitch);

        abstract boolean isAlive();
    }

    private static class ControllerAt extends Controller {
        private final float[] initPos;
        private final float[] initRot;

        ControllerAt(float[] initPos, float[] initRot) {
            this.initPos = initPos;
            this.initRot = initRot;
        }

        @Nonnull
        @Override
        public Matrix4f initMatrix(float partial, float x, float y, float z, float yaw, float pitch) {
            return new Matrix4f().identity()
                    .translatef(initPos[0] + x, initPos[1] + y, initPos[2] + z)
                    .rotateMC(initRot[0] + yaw + 90, initRot[1] + pitch);
        }

        @Nullable
        @Override
        public Matrix4f updateMatrix(float partial, float x, float y, float z, float yaw, float pitch) {
            return null;
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }

    private static class ControllerWith extends Controller {
        private final boolean followX, followY, followZ, followYaw, followPitch;
        private final boolean useHead, useRender;
        private final boolean inheritYaw, inheritPitch;
        private final int entityId;
        private final boolean asAt;
        private double[] initPos;
        private float[] initAngle;

        private final EntityConvert convert;

        ControllerWith(EntityConvert convert, int entityId,
                       boolean followX, boolean followY, boolean followZ, boolean followYaw, boolean followPitch,
                       boolean inheritYaw, boolean inheritPitch, boolean useHead, boolean useRender) {
            this.convert = convert;
            this.entityId = entityId;

            this.followX = followX;
            this.followY = followY;
            this.followZ = followZ;
            this.followYaw = followYaw;
            this.followPitch = followPitch;

            this.inheritYaw = inheritYaw;
            this.inheritPitch = inheritPitch;

            this.useHead = useHead;
            this.useRender = useRender;

            this.asAt = !(followX || followY || followZ || followYaw || followPitch);
        }

        private float[] getRotation() {
            if (useHead && convert.canUseHead(entityId)) {
                return convert.getHeadRotation(entityId);
            } else if (useRender && convert.canUseRender(entityId)) {
                return convert.getRenderRotation(entityId);
            } else {
                return convert.getRotation(entityId);
            }
        }

        private float[] getPrevRotation() {
            if (useHead && convert.canUseHead(entityId)) {
                return convert.getPrevHeadRotation(entityId);
            } else if (useRender && convert.canUseRender(entityId)) {
                return convert.getPrevRenderRotation(entityId);
            } else {
                return convert.getPrevRotation(entityId);
            }
        }

        @Nonnull
        @Override
        public Matrix4f initMatrix(float partial, float x, float y, float z, float yaw, float pitch) {
            if (!convert.isValid(entityId))
                return new Matrix4f().identity();

            initPos = convert.getPosition(entityId);
            initAngle = getRotation();

            float[] rotationYaw = new float[]{
                    yaw + (inheritYaw ? initAngle[0] + 90 : 0),
                    pitch + (inheritPitch ? initAngle[1] : 0)
            };

            return new Matrix4f().identity()
                    .translated(initPos[0] + x, initPos[1] + y, initPos[2] + z)
                    .rotateMC(rotationYaw[0], rotationYaw[1]);
        }

        @Nullable
        @Override
        public Matrix4f updateMatrix(float partial, float x, float y, float z, float yaw, float pitch) {

            if (asAt || !convert.isValid(entityId))
                return null;

            double d0, d1, d2;
            float d3, d4;

            // position
            {
                double[] curPos = convert.getPosition(entityId), prevPos = convert.getPrevPosition(entityId);
                d0 = followX ? prevPos[0] + (curPos[0] - prevPos[0]) * partial : initPos[0];
                d1 = followY ? prevPos[1] + (curPos[1] - prevPos[1]) * partial : initPos[1];
                d2 = followZ ? prevPos[2] + (curPos[2] - prevPos[2]) * partial : initPos[2];
            }

            // rotation
            {
                float[] curRot = getRotation(), prevRot = getPrevRotation();

                if (followYaw) {
                    d3 = prevRot[0] + (curRot[0] - prevRot[0]) * partial;
                    d3 += inheritYaw ? 90 : -initAngle[0];
                } else {
                    d3 = inheritYaw ? initAngle[0] + 90 : 0;
                }

                if (followPitch) {
                    d4 = prevRot[1] + (curRot[1] - prevRot[1]) * partial;
                    if (!inheritPitch) d4 -= initAngle[1];
                } else {
                    d4 = inheritPitch ? initAngle[1] : 0;
                }
            }

            return new Matrix4f().identity()
                    .translated(d0 + x, d1 + y, d2 + z)
                    .rotateMC(d3 + yaw, d4 + pitch);
        }

        @Override
        public boolean isAlive() {
            return asAt || convert.isValid(entityId) && convert.isAlive(entityId);
        }
    }

}
