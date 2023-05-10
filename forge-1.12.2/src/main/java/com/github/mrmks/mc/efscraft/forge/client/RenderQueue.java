package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class RenderQueue {

    private final Queue<Entry> present = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<String, Entry>> lookup = new ConcurrentHashMap<>();
    private final AtomicBoolean clearMark = new AtomicBoolean(false);

    private final Function<String, EfsEffect> effects;

    RenderQueue(Function<String, EfsEffect> effects) {
        this.effects = effects;
    }

    private void putEntry(Entry entry, boolean overwrite) {

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.effect, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (overwrite || old == null) {
            present.add(entry);
            submap.put(entry.emitter, entry);

            if (old != null) old.state = State.STOPPING;
        }
    }

    void commandPlay(String key, String effect, String emitter, int lifespan, int skip, Matrix4f local,
                     float[] modelPos, float[] modelRot, float[] dynamic, Controller controller, boolean overwrite)
    {
        if (clearMark.get()) return;
        Entry entry = new Entry(key, effect, emitter, lifespan, skip, local, modelPos, modelRot, dynamic);
        entry.setController(controller);

        putEntry(entry, overwrite);
    }

    void commandStop(String key, String emitter) {
        if (clearMark.get()) return;

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

                if (entry.state == State.STOPPING) {
                    entry.stop();
                }

                if (entry.state == State.STOPPED) {
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

        Entry entry = new Entry("Laser03", "Laser03", "debug", 203, 0, local, new float[3], new float[2], null);
        Controller controller = new ControllerAt(new float[] {0, 10, 0}, new float[2]);

        entry.setController(controller);

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.effect, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (old != null) old.state = State.STOPPING;

        submap.put(entry.emitter, entry);
        present.add(entry);
    }

    enum State { NEW, CREATED, RUNNING, STOPPING, STOPPED }

    private static class Entry {

        final String key, effect, emitter;
        private final int skipFrame, lifespan;
        private final Matrix4f local;
        protected final float[] rotModel, posModel;
        private final float[] dynamics;

        private Controller controller;

        private EfsEffectHandle handle;
        private float lifeLength;
        transient RenderQueue.State state;
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
            state = RenderQueue.State.NEW;
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
            state = RenderQueue.State.CREATED;
        }

        void update(float frames, float partial) {

            if (lifeLength >= lifespan || !handle.exists() || !controller.isAlive())
                state = RenderQueue.State.STOPPING;
            else {
                lifeLength += frames;
                if (state == RenderQueue.State.CREATED) {

                    Matrix4f matrix4f = controller.initMatrix(partial, posModel[0], posModel[1], posModel[2], rotModel[0], rotModel[1]).mul(local);
                    handle.setBaseTransformMatrix(matrix4f.getFloats());

                    state = RenderQueue.State.RUNNING;
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
            state = RenderQueue.State.STOPPED;
        }
    }

    interface Controller {
        @Nonnull Matrix4f initMatrix(float partial, float x, float y, float z, float yaw, float pitch);
        @Nullable Matrix4f updateMatrix(float partial, float x, float y, float z, float yaw, float pitch);

        boolean isAlive();
    }

    static class ControllerAt implements Controller {
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
                    .rotateMC( initRot[0] + yaw + 90, initRot[1] + pitch);
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

    static class ControllerWith implements Controller {
        private final boolean followX, followY, followZ, followYaw, followPitch;
        private final boolean useHead, useRender;
        private final boolean inheritYaw, inheritPitch;
        private final int entityId;
        private final boolean asAt;
        private double[] initPos;
        private float[] initAngle;

        ControllerWith(int entityId,
                boolean followX, boolean followY, boolean followZ, boolean followYaw, boolean followPitch,
                boolean inheritYaw, boolean inheritPitch, boolean useHead, boolean useRender)
        {
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

        private Entity findEntity() {
            Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
            return entity != null && entity.isEntityAlive() && entity.isAddedToWorld() ? entity : null;
        }

        private float getYaw(Entity entity) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase base = (EntityLivingBase) entity;

                return useHead ? base.getRotationYawHead() : useRender ? base.renderYawOffset : base.rotationYaw;
            } else {
                return useHead ? entity.getRotationYawHead() : entity.rotationYaw;
            }
        }

        private float getPitch(Entity entity) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase base = (EntityLivingBase) entity;

                return useRender ? base.cameraPitch : base.rotationPitch;
            } else {
                return entity.rotationPitch;
            }
        }

        @Nonnull
        @Override
        public Matrix4f initMatrix(float partial, float x, float y, float z, float yaw, float pitch) {
            Entity entity = findEntity();
            if (entity == null) return new Matrix4f().identity();

            initPos = new double[] {entity.posX, entity.posY, entity.posZ};
            initAngle = new float[] {getYaw(entity), getPitch(entity)};

            float[] rotationYaw = new float[] {
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
            Entity entity = findEntity();
            if (asAt || entity == null) return null;

            double d0, d1, d2; float d3, d4;
            d0 = followX ? entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial : initPos[0];
            d1 = followY ? entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial : initPos[1];
            d2 = followZ ? entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial : initPos[2];

            if (followYaw) {
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase base = (EntityLivingBase) entity;

                    if (useHead) {
                        d3 = base.prevRotationYawHead + (base.rotationYawHead - base.prevRotationYawHead) * partial;
                    } else if (useRender) {
                        d3 = base.prevRenderYawOffset + (base.renderYawOffset - base.prevRenderYawOffset) * partial;
                    } else {
                        d3 = base.prevRotationYaw + (base.rotationYaw - base.prevRotationYaw) * partial;
                    }
                } else {
                    d3 = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partial;
                }

                d3 += inheritYaw ? 90 : -initAngle[0];
            } else {
                d3 = inheritYaw ? initAngle[0] + 90 : 0;
            }

            if (followPitch) {
                if (entity instanceof EntityLivingBase && useRender) {
                    EntityLivingBase base = (EntityLivingBase) entity;

                    d4 = base.prevCameraPitch + (base.cameraPitch - base.prevCameraPitch) * partial;
                } else {
                    d4 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partial;
                }

                if (!inheritPitch) d4 -= initAngle[1];
            } else {
                d4 = inheritPitch ? initAngle[1] : 0;
            }

            return new Matrix4f().identity()
                    .translated(d0 + x, d1 + y, d2 + z)
                    .rotateMC(d3 + yaw, d4 + pitch);
        }

        @Override
        public boolean isAlive() {
            return asAt || findEntity() != null;
        }
    }
}
