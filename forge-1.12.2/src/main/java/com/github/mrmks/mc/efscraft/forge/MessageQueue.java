package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.efkseer4j.EfsEffectHandle;
import com.github.mrmks.efkseer4j.EfsProgram;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;
import com.github.mrmks.mc.efscraft.packet.SPacketStop;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

class MessageQueue {

    private final Queue<Entry> present = new ConcurrentLinkedQueue<>();
    private final Map<String, Map<String, Entry>> lookup = new ConcurrentHashMap<>();
    private final Map<UUID, Queue<Entry>> tracing = new ConcurrentHashMap<>();
    private final AtomicBoolean clearMark = new AtomicBoolean(false);

    private final Function<String, EfsEffect> effects;

    MessageQueue(Function<String, EfsEffect> effects) {
        this.effects = effects;
    }

    void processWith(SPacketPlayWith play) {

        if (clearMark.get()) return;

        Entry entry = new EntryWith(play);

        UUID target = play.getTarget();
        Queue<Entry> queue = tracing.get(target);
        if (queue == null)
            tracing.put(target, queue = new ConcurrentLinkedQueue<>());

        Map<String, Entry> submap = lookup.get(entry.effect);
        if (submap == null)
            lookup.put(entry.effect, submap = new ConcurrentHashMap<>());

        Entry old = submap.get(entry.emitter);

        if (play.conflictOverwrite() || old == null) {
            queue.add(entry);
            present.add(entry);
            submap.put(entry.emitter, entry);

            if (old != null) old.markStopping = true;
        }
    }

    void processAt(SPacketPlayAt play) {

        if (clearMark.get()) return;

        Entry entry = new EntryAt(play);

        Map<String, Entry> submap = lookup.computeIfAbsent(entry.effect, it -> new ConcurrentHashMap<>());
        Entry old = submap.get(entry.emitter);

        if (play.conflictOverwrite() || old == null) {
            present.add(entry);
            submap.put(entry.emitter, entry);

            if (old != null) old.markStopping = true;
        }
    }

    void processStop(SPacketStop stop) {

        if (clearMark.get()) return;

        Map<String, Entry> lookup = this.lookup.get(stop.getEffect());
        if (lookup != null) {
            Entry entry = lookup.get(stop.getEmitter());
            if (entry != null) {
                entry.markStopping = true;
            }
        }
    }

    void processClear(SPacketClear clear) {
        clearMark.set(true);
    }

    void update(float partial, EfsProgram program) {
        // process pending commands;
        if (clearMark.get()) {
            present.clear();
            lookup.clear();
            tracing.clear();

            program.stopEffects();
            clearMark.set(false);
        }

        // check effects lifespan;
        {
            Iterator<Entry> iterator = present.iterator();
            while (iterator.hasNext()) {
                Entry node = iterator.next();
                node.lifeLength += partial;

                if (node.effectHandle == null) {
                    EfsEffect effect = effects.apply(node.effect);
                    if (effect == null) {
                        iterator.remove();
                        continue;
                    } else {
                        node.effectHandle = program.playEffect(effect);
                        node.effectHandle.setProgress(node.skipFrame);
                    }
                }

                if (!node.needEntity()) {
                    if (!node.markInit) {
                        node.effectHandle.setBaseTransformMatrix(node.initModel(null));
                        node.markInit = true;
                    } else {
                        node.updateModel(partial, null, node.effectHandle::setBaseTransformMatrix);
                    }
                }

                if (!node.effectHandle.exists()) {
                    node.markStopping = node.markStopped = true;
                } else {
                    node.markStopping |= node.lifeLength >= node.lifespan;

                    if (node.markStopping && !node.markStopped) {
                        node.effectHandle.stop();
                        node.markStopped = true;
                    }
                }

                if (node.markStopped) {
                    iterator.remove();

                    lookup.get(node.effect).remove(node.emitter);
                }
            }
        }

        // update tracing effects;
        {
            List<Entity> entities = Minecraft.getMinecraft().world.loadedEntityList;
            for (Entity entity : entities) {
                if (!entity.isAddedToWorld()) continue;

                UUID uuid = entity.getPersistentID();
                Queue<Entry> list = tracing.get(uuid);
                if (list == null || list.isEmpty()) continue;

                list.removeIf(node -> node.markStopped);

                for (Entry node : list) {

                    if (!node.markInit) {
                        node.effectHandle.setBaseTransformMatrix(node.initModel(entity));
                        node.markInit = true;
                    } else {
                        node.updateModel(partial, entity, node.effectHandle::setBaseTransformMatrix);
                    }

                }
            }
        }
    }

    void stopAll() {
        present.clear();
        lookup.clear();
        tracing.clear();

        clearMark.set(true);
    }

    void createDebug(EfsProgram program, double x, double y, double z) {
        EfsEffect effect = effects.apply("Laser03");
        if (effect != null) {
            EfsEffectHandle handle = program.playEffect(effect);
//            handle.moveTo((float) x, (float) y, (float) z);
            handle.moveTo(0, 3, 0);
        }
    }

    private static Matrix4f createLocal(SPacketPlayWith play) {
        return new Matrix4f().identity()
//                .rotate(play.getRotationModel()[0], play.getRotationModel()[1])
                .translatef(play.getPositionLocal()[0], play.getPositionLocal()[1], play.getPositionLocal()[2])
                .rotateMC(play.getRotationLocal()[0], play.getRotationLocal()[1])
                .scale(play.getScale()[0], play.getScale()[1], play.getScale()[2]);
    }

    private static Matrix4f createLocal(SPacketPlayAt play) {
        return new Matrix4f().identity()
//                .rotate(play.getRotationModel()[0], play.getRotationModel()[1])
                .translatef(play.getPositionLocal()[0], play.getPositionLocal()[1], play.getPositionLocal()[2])
                .rotateMC(play.getRotationLocal()[0], play.getRotationLocal()[1])
                .scale(play.getScale()[0], play.getScale()[1], play.getScale()[2]);
    }

    private abstract static class Entry {
        final String effect, emitter;
        final int skipFrame, lifespan;
        final Matrix4f local;
        final float[] rotModel;
        final float[] posModel;
        float lifeLength;
        EfsEffectHandle effectHandle;

        transient boolean markStopped;
        transient boolean markStopping;
        transient boolean markInit;

        Entry(String effect, String emitter, int skip, int lifespan, Matrix4f local, float[] rotModel, float[] posModel) {
            this.effect = effect;
            this.emitter = emitter;
            this.skipFrame = skip;
            this.lifespan = lifespan;

            this.local = local;
            this.rotModel = rotModel;
            this.posModel = posModel;

            this.lifeLength = 0;
            this.effectHandle = null;
            this.markStopped = this.markStopping = this.markInit = false;
        }

        abstract boolean needEntity();
        abstract float[] initModel(Entity entity);
        abstract void updateModel(float partial, Entity entity, Consumer<float[]> con);
    }

    private static class EntryAt extends Entry {
        private final float[] initPos;
        EntryAt(SPacketPlayAt play) {
            super(play.getEffect(), play.getEmitter(), play.getFrameSkip(), play.getLifespan(), createLocal(play), play.getRotationModel(), play.getPositionModel());

            initPos = play.getModelPos();
        }

        @Override
        boolean needEntity() {
            return false;
        }

        @Override
        float[] initModel(Entity entity) {
            return new Matrix4f().identity()
                    .translatef(initPos[0], initPos[1], initPos[2])
                    .translatef(posModel[0], posModel[1], posModel[2])
                    .rotateMC(rotModel[0], rotModel[1])
                    .mul(local)
                    .getFloats();
        }

        @Override
        void updateModel(float partial, Entity entity, Consumer<float[]> con) {}
    }

    private static class EntryWith extends Entry {
        private final boolean followX, followY, followZ, followYaw, followPitch;
        private final boolean asAt;
        private double[] initPos;
        private float[] initAngle;
        EntryWith(SPacketPlayWith play) {
            super(play.getEffect(), play.getEmitter(), play.getFrameSkip(), play.getLifespan(), createLocal(play), play.getRotationModel(), play.getPositionModel());

            followX = play.followX();
            followY = play.followY();
            followZ = play.followZ();
            followYaw = play.followYaw();
            followPitch = play.followPitch();

            asAt = !(followX || followY || followZ || followYaw || followPitch);
        }

        @Override
        boolean needEntity() {
            return true;
        }

        float[] initModel(Entity entity) {
            if (entity == null) {
                return new Matrix4f().identity().getFloats();
            } else {
                initPos = new double[] {entity.posX, entity.posY, entity.posZ};
                initAngle = new float[] {entity.rotationYaw, entity.rotationPitch};
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase base = (EntityLivingBase) entity;
                    initAngle[0] = base.renderYawOffset;
                    initAngle[1] = base.prevCameraPitch;
                }
                return new Matrix4f().identity()
                        .translated(initPos[0], initPos[1], initPos[2])
                        .translatef(posModel[0], posModel[1], posModel[2])
                        .rotateMC(initAngle[0] + 90, initAngle[1])
                        .rotateMC(rotModel[0], rotModel[1])
                        .mul(local)
                        .getFloats();
            }
        }

        @Override
        void updateModel(float partial, Entity entity, Consumer<float[]> con) {
            if (!asAt) {
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

                Matrix4f matrix4f = new Matrix4f().identity()
                        .translated(d0, d1, d2)
                        .translatef(posModel[0], posModel[1], posModel[2])
                        .rotateMC(d3 + 90, d4)
                        .rotateMC(rotModel[0], rotModel[1])
                        .mul(local);

                con.accept(matrix4f.getFloats());
            }
        }
    }
}
