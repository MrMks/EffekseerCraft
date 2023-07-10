package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.math.Matrix4f;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class RenderingQueue<ENTITY> {

    // a mask that will clear the queue and prevent any further render;
    private final AtomicBoolean clearMark = new AtomicBoolean(false);
    // entries queue
    private final Queue<Entry> present = new ConcurrentLinkedQueue<>();
    // lookup map for searching.
    private final Map<String, Map<String, Entry>> lookup = new ConcurrentHashMap<>();

    // client effect registry
    private final Function<String, EfsEffect> effects;
    // a version specific adaptor
    private final EntityConvert<ENTITY> convert;
    private final ILogAdaptor logger;

    public RenderingQueue(Function<String, EfsEffect> getter, EntityConvert<ENTITY> convert, ILogAdaptor logger) {
        this.effects = getter;
        this.convert = convert;
        this.logger = logger;
    }

    private void putEntry(String key, String emitter, String effect, boolean overwrite,
                          Predicate predicate, Updater initializer, Updater updater
    ) {
        Map<String, Entry> submap = lookup.computeIfAbsent(key, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(emitter);

        if (overwrite || old == null) {

            Entry entry = new Entry(key, emitter, effect, initializer, updater, predicate);

            present.add(entry);
            submap.put(emitter, entry);

            if (old != null)
                old.state = State.STOPPING_OVERWRITE;
        }
    }

    void commandPlayWith(String key, String emitter, String effect, boolean overwrite,
                         int skip, int lifespan, float[] dynamics,
                         Matrix4f base, Vec3f modelPos, Vec2f modelRot,
                         int eid,
                         boolean fx, boolean fy, boolean fz, boolean fw, boolean fp,
                         boolean iw, boolean ip,
                         boolean useHead, boolean useRender
    ) {
        ENTITY entity = convert.findEntity(eid);
        if (entity == null || !convert.isAlive(entity)) return;

        boolean asAt = !(fx || fy || fz || fw || fp);
        Predicate predicate = (h, l) -> {
            ENTITY ins = convert.findEntity(eid);
            return h.exists() && l < lifespan && ins != null && convert.isAlive(ins);
        };
        if (asAt) {
            Vec3f targetPos = convert.getPosition(entity);
            Vec2f targetRot;
            if (!iw && !ip) {
                targetRot = new Vec2f(-90, 0);
            } else {
                if (useHead) {
                    targetRot = convert.getHeadRotation(entity);
                } else if (useRender) {
                    targetRot = convert.getRenderRotation(entity);
                } else {
                    targetRot = convert.getRotation(entity);
                }
                targetRot = new Vec2f(iw ? targetRot.x() : -90, ip ? targetRot.y() : 0);
            }

            float[] one = new Matrix4f().identity()
                    .translatef(new Vec3f(modelPos).add(targetPos))
                    .rotateMC(new Vec2f(90, 0).add(modelRot).add(targetRot))
                    .mul(base)
                    .getFloats();
            Updater initializer = (h, p) -> {
                h.setBaseTransformMatrix(one);
                h.setProgress(skip);
                if (dynamics != null) for (int i = 0; i < dynamics.length; i++) h.setDynamicInput(i, dynamics[i]);
            };

            putEntry(key, emitter, effect, overwrite, predicate, initializer, null);
        } else {

            final Vec3f initPos = convert.getPosition(entity);
            final Vec2f initRot;
            {
                if (useHead && convert.canUseHead(entity)) {
                    initRot = convert.getHeadRotation(entity);
                } else if (useRender && convert.canUseRender(entity)) {
                    initRot = convert.getRenderRotation(entity);
                } else {
                    initRot = convert.getRotation(entity);
                }
            }

            Updater updater = (h, p) -> {
                Vec3f targetPos; Vec2f targetRot;

                {
                    Vec3f cur = convert.getPosition(entity);
                    Vec3f pre = convert.getPrevPosition(entity);
                    
                    targetPos = pre.copy().linearTo(cur, p);
                    
                    targetPos = new Vec3f(
                            fx ? targetPos.x() : initPos.x(),
                            fy ? targetPos.y() : initPos.y(),
                            fz ? targetPos.z() : initPos.z()
                    );
                }

                if (fw || fp || iw || ip)
                {
                    Vec2f cur, pre;
                    if (useHead && convert.canUseHead(entity)) {
                        cur = convert.getHeadRotation(entity); pre = convert.getPrevHeadRotation(entity);
                    } else if (useRender && convert.canUseRender(entity)) {
                        cur = convert.getRenderRotation(entity); pre = convert.getPrevRenderRotation(entity);
                    } else {
                        cur = convert.getRotation(entity); pre = convert.getPrevRotation(entity);
                    }

                    Vec2f tmp = new Vec2f(pre).linearTo(cur, p).minus(initRot);
                    tmp = new Vec2f(fw ? tmp.x() : 0, fp ? tmp.y() : 0);
                    tmp.add(iw ? initRot.x() : 0, ip ? initRot.y() : 0);

                    if (iw) tmp.add(90, 0);

                    targetRot = tmp;
                } else {
                    targetRot = new Vec2f();
                }

                float[] one = new Matrix4f().identity()
                        .translatef(new Vec3f(modelPos).add(targetPos))
                        .rotateMC(new Vec2f(modelRot).add(targetRot))
                        .mul(base)
                        .getFloats();

                h.setBaseTransformMatrix(one);
            };

            Updater initializer = (h, p) -> {
                updater.accept(h, p);
                h.setProgress(skip);
                if (dynamics != null) for (int i = 0; i < dynamics.length; i ++) h.setDynamicInput(i, dynamics[i]);
            };

            putEntry(key, emitter, effect, overwrite, predicate, initializer, updater);
        }
    }

    void commandPlayAt(String key, String emitter, String effect, boolean overwrite,
                       int skip, int lifespan, float[] dynamics,
                       Matrix4f base, Vec3f modelPos, Vec2f modelRot,
                       Vec3f targetPos, Vec2f targetRot
    ) {
        Predicate predicate = (h, l) -> h.exists() && l < lifespan;

        float[] one = new Matrix4f().identity()
                .translatef(new Vec3f().add(modelPos).add(targetPos))
                .rotateMC(new Vec2f(90, 0).add(modelRot).add(targetRot))
                .mul(base)
                .getFloats();

        Updater initializer = (h, l) -> {
            h.setBaseTransformMatrix(one);
            h.setProgress(skip);
            if (dynamics != null) for (int i = 0; i < dynamics.length; i++) h.setDynamicInput(i, dynamics[i]);
        };

        putEntry(key, emitter, effect, overwrite, predicate, initializer, null);
    }

    void commandStop(String key, String emitter) {
        if (clearMark.get()) return;

        Map<String, Entry> submap = this.lookup.get(key);
        if (submap != null) {
            if (emitter.isEmpty() || "*".equals(emitter)) {
                submap.values().forEach(it -> it.state = State.STOPPING);
            } else {
                Entry entry = submap.get(emitter);
                if (entry != null) {
                    entry.state = State.STOPPING;
                }
            }
        }
    }

    void commandTrigger(String key, String emitter, final int id) {
        if (clearMark.get()) return;

        Entry entry = lookup.getOrDefault(key, Collections.emptyMap()).get(emitter);

        if (entry != null)
            entry.preTasks.add((h, p) -> h.sendTrigger(id));
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
                        logger.logWarning("Unable to player an absent effect with key: " + entry.effect);
                    } else {
                        entry.init(program.playEffect(effect));
                        logger.logDebug("Begin to play effect: " + "[key: " + entry.effect + ", emitter: " + entry.emitter + "]");
                    }
                }

                if (entry.state == State.CREATED || entry.state == State.RUNNING) {
                    entry.update(frameGap, partial);
                }

                if (entry.state == State.STOPPING || entry.state == State.STOPPING_OVERWRITE) {
                    entry.stop();
                }

                if (entry.state == State.STOPPED) {
                    lookup.getOrDefault(entry.key, Collections.emptyMap()).remove(entry.emitter);
                    return true;
                } else return entry.state == State.STOPPED_OVERWRITE;
            });
        }
    }

    void stopAll() {
        present.clear();
        lookup.clear();

        clearMark.set(true);
    }

    void createDebug() {

        String key = "<DEBUG>", emitter = "<DEBUG>";

        if (lookup.getOrDefault(key, Collections.emptyMap()).containsKey(emitter))
            return;

        Matrix4f local = new Matrix4f().identity();
        Vec3f pos = new Vec3f();
        Vec2f rot = new Vec2f();

        commandPlayAt(key, emitter, "laser03", true,
                0, 203, null,
                local, pos, rot, pos, rot);
    }

    enum State { NEW, CREATED, RUNNING, STOPPING, STOPPING_OVERWRITE, STOPPED, STOPPED_OVERWRITE }

    interface Updater { void accept(EfsEffectHandle handle, float partial); }
    interface Predicate { boolean test(EfsEffectHandle handle, float lifeLength); }

    private static class Entry {
        final String key, emitter, effect;
        final Predicate predicate;
        final Updater initializer, updater;

        final Queue<Updater> preTasks = new ConcurrentLinkedQueue<>(), postTasks = new ConcurrentLinkedQueue<>();

        private float lifeLength;

        private EfsEffectHandle handle;
        transient State state;

        Entry(String key, String emitter, String effect, Updater initializer, Updater updater, Predicate predicate) {
            this.key = key;
            this.emitter = emitter;
            this.effect = effect;

            this.initializer = initializer;
            this.updater = updater;
            this.predicate = predicate;

            state = State.NEW;
        }

        void init(EfsEffectHandle handle) {
            this.handle = handle;

            state = State.CREATED;
        }

        void update(float frames, float partial) {
            if (!predicate.test(handle, lifeLength))
                state = State.STOPPING;
            else {
                lifeLength += frames;
                while (!preTasks.isEmpty()) preTasks.poll().accept(handle, partial);

                if (state == State.CREATED) {
                    if (initializer != null) initializer.accept(handle, partial);
                    state = State.RUNNING;
                } else if (state == State.RUNNING) {
                    if (updater != null) updater.accept(handle, partial);
                }

                while (!postTasks.isEmpty()) postTasks.poll().accept(handle, partial);
            }
        }

        void stop() {
            if (handle != null)
                handle.stop();
            if (state == State.STOPPING)
                state = State.STOPPED;
            else if (state == State.STOPPING_OVERWRITE)
                state = State.STOPPED_OVERWRITE;
        }
    }

}
