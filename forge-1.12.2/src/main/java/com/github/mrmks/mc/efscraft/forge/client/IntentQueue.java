package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class IntentQueue {

    private final Queue<Entry> present = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<String, Entry>> lookup = new ConcurrentHashMap<>();
    private final AtomicBoolean clearMark = new AtomicBoolean(false);

    private final Function<String, EfsEffect> effects;

    IntentQueue(Function<String, EfsEffect> effects) {
        this.effects = effects;
    }

    void processWith(SPacketPlayWith play) {

        if (clearMark.get()) return;

        Entry entry = new EntryWith(play);
        putEntry(entry, play.conflictOverwrite());
    }

    void processAt(SPacketPlayAt play) {

        if (clearMark.get()) return;

        Entry entry = new EntryAt(play);
        putEntry(entry, play.conflictOverwrite());
    }

    private void putEntry(Entry entry, boolean overwrite) {

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.effect, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (overwrite || old == null) {
            present.add(entry);
            submap.put(entry.emitter, entry);

            if (old != null) old.state = Entry.State.STOPPING;
        }

    }

    void processStop(SPacketStop stop) {

        if (clearMark.get()) return;

        Map<String, Entry> lookup = this.lookup.get(stop.getKey());
        if (lookup != null) {
            String emitter = stop.getEmitter();
            if (emitter.isEmpty()) {
                lookup.values().forEach(it -> it.state = Entry.State.STOPPING);
            } else {
                Entry entry = lookup.get(emitter);
                if (entry != null) {
                    entry.state = Entry.State.STOPPING;
                }
            }
        }
    }

    void processClear(SPacketClear clear) {
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
                if (entry.state == Entry.State.NEW) {
                    EfsEffect effect = effects.apply(entry.effect);
                    if (effect == null) {
                        entry.state = Entry.State.STOPPED;
                    } else {
                        entry.init(program.playEffect(effect));
                    }
                }

                if (entry.state == Entry.State.CREATED || entry.state == Entry.State.RUNNING) {
                    entry.update(frameGap, partial);
                }

                if (entry.state == Entry.State.STOPPING) {
                    entry.stop();
                }

                if (entry.state == Entry.State.STOPPED) {
                    lookup.getOrDefault(entry.effect, Collections.emptyMap()).remove(entry.emitter);
                    return true;
                }

                return false;
            });
        }

    }

    void stopAll() {
        present.clear();
        lookup.clear();

        clearMark.set(true);
    }

    void createDebug() {
        Matrix4f local = new Matrix4f().identity();
        float[] rotModel = new float[2];
        float[] posModel = new float[] {0, 10, 0};

        Entry entry = new Entry("Laser03", "Laser03", "debug", 0, 203, local, rotModel, posModel) {
            @Override
            Matrix4f initMatrix(float partial) {
                return new Matrix4f().identity()
                        .translatef(posModel[0], posModel[1], posModel[2])
                        .rotateMC(rotModel[0] + 90, rotModel[1])
                        ;
            }

            @Override
            Matrix4f updateMatrix(float partial) {
                return null;
            }

            @Override
            boolean isAlive() {
                return true;
            }
        };

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.effect, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (old != null) old.state = Entry.State.STOPPING;

        submap.put(entry.emitter, entry);
        present.add(entry);
    }

    private abstract static class Entry {

        enum State { NEW, CREATED, RUNNING, STOPPING, STOPPED }

        final String key, effect, emitter;
        private final int skipFrame, lifespan;
        private final Matrix4f local;
        protected final float[] rotModel, posModel;

        private EfsEffectHandle handle;
        private float lifeLength;
        transient State state;

        Entry(SPacketPlayAbstract play) {
            this.key = play.getKey();
            this.effect = play.getEffect();
            this.emitter = play.getEmitter();
            this.skipFrame = play.getFrameSkip();
            this.lifespan = play.getLifespan();

            float[] posLocal = play.getLocalPosition();
            float[] rotLocal = play.getLocalRotation();
            float[] scale = play.getScale();

            local = new Matrix4f().identity()
                    .translatef(posLocal[0], posLocal[1], posLocal[2])
                    .rotateMC(rotLocal[0], rotLocal[1])
                    .scale(scale[0], scale[1], scale[2]);
            this.rotModel = play.getModelRotation();
            this.posModel = play.getModelPosition();

            this.lifeLength = 0;
            this.handle = null;
            state = State.NEW;
        }

        Entry(String key, String effect, String emitter, int skip, int lifespan, Matrix4f local, float[] rotModel, float[] posModel) {
            this.key = key;
            this.effect = effect;
            this.emitter = emitter;
            this.skipFrame = skip;
            this.lifespan = lifespan;

            this.local = local;
            this.rotModel = rotModel;
            this.posModel = posModel;

            this.lifeLength = 0;
            this.handle = null;
            state = State.NEW;
        }

        void init(EfsEffectHandle handle) {
            this.handle = handle;
            handle.setProgress(skipFrame);
            state = State.CREATED;
        }

        void update(float frames, float partial) {

            if (lifeLength >= lifespan || !handle.exists() || !isAlive())
                state = State.STOPPING;
            else {
                lifeLength += frames;
                if (state == State.CREATED) {
                    handle.setBaseTransformMatrix(initMatrix(partial).mul(local).getFloats());
                    state = State.RUNNING;
                } else {
                    Matrix4f matrix4f = updateMatrix(partial);
                    if (matrix4f != null) {
                        handle.setBaseTransformMatrix(matrix4f.mul(local).getFloats());
                    }
                }
            }

        }

        abstract Matrix4f initMatrix(float partial);
        abstract Matrix4f updateMatrix(float partial);
        abstract boolean isAlive();

        void stop() {
            if (handle != null)
                handle.stop();
            state = State.STOPPED;
        }
    }

    private static class EntryAt extends Entry {
        private final float[] initPos;
        EntryAt(SPacketPlayAt play) {
            super(play);
            initPos = play.getModelPos();
        }

        @Override
        Matrix4f initMatrix(float partial) {
            return new Matrix4f().identity()
                    .translatef(initPos[0], initPos[1], initPos[2])
                    .translatef(posModel[0], posModel[1], posModel[2])
                    .rotateMC(rotModel[0] + 90, rotModel[1]);

        }

        @Override
        Matrix4f updateMatrix(float partial) {
            return null;
        }

        @Override
        boolean isAlive() {
            return true;
        }
    }

    private static class EntryWith extends Entry {
        private final boolean followX, followY, followZ, followYaw, followPitch;
        private final int entityId;
        private final boolean asAt;
        private double[] initPos;
        private float[] initAngle;
        EntryWith(SPacketPlayWith play) {
            super(play);

            followX = play.followX();
            followY = play.followY();
            followZ = play.followZ();
            followYaw = play.followYaw();
            followPitch = play.followPitch();
            entityId = play.getTarget();

            asAt = !(followX || followY || followZ || followYaw || followPitch);
        }

        private Entity findEntity() {
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
            return entity != null && entity.isEntityAlive() && entity.isAddedToWorld() ? entity : null;
        }

        @Override
        Matrix4f initMatrix(float partial) {
            Entity entity = findEntity();
            if (entity == null) return new Matrix4f().identity();

            initPos = new double[] {entity.posX, entity.posY, entity.posZ};
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase base = (EntityLivingBase) entity;
                initAngle = new float[] {base.renderYawOffset, base.cameraPitch};
            } else {
                initAngle = new float[] {entity.rotationYaw, entity.rotationPitch};
            }

            return new Matrix4f().identity()
                    .translated(initPos[0], initPos[1], initPos[2])
                    .translated(posModel[0], posModel[1], posModel[2])
                    .rotateMC(initAngle[0] + 90 + rotModel[0], initAngle[1] + rotModel[1]);
        }

        @Override
        Matrix4f updateMatrix(float partial) {

            Entity entity = findEntity();
            if (asAt || entity == null) return null;

            double d0, d1, d2; float d3, d4;
            d0 = followX ? entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial : initPos[0];
            d1 = followY ? entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial : initPos[1];
            d2 = followZ ? entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial : initPos[2];
            d3 = followYaw ? (entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partial) : initAngle[0];
            d4 = followPitch ? entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partial : initAngle[1];

            if (entity instanceof EntityLivingBase) {
                EntityLivingBase base = (EntityLivingBase) entity;
                if (followYaw) d3 = base.prevRenderYawOffset + (base.renderYawOffset - base.prevRenderYawOffset) * partial;
                if (followPitch) d4 = base.prevCameraPitch + (base.cameraPitch - base.prevCameraPitch) * partial;
            }

            return new Matrix4f().identity()
                    .translated(d0, d1, d2)
                    .translated(posModel[0], posModel[1], posModel[2])
                    .rotateMC(d3 + rotModel[0] + 90, d4 + rotModel[1]);
        }

        @Override
        boolean isAlive() {
            return asAt || findEntity() != null;
        }
    }
}
