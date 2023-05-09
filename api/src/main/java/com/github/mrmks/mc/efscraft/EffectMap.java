package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.SPacketPlayAbstract;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;
import com.github.mrmks.mc.efscraft.packet.SPacketStop;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EffectMap {

    public static class Pojo {
        @SerializedName("extendsFrom") String parent;
        @SerializedName("effect") String effect;
        @SerializedName("lifespan") Integer lifespan;
        @SerializedName("skipFrame") Integer skipFrame;
        @SerializedName("scale") float[] scale;
        @SerializedName("rotateLocal") float[] rotateLocal;
        @SerializedName("rotateModel") float[] rotateModel;
        @SerializedName("translateLocal") float[] posLocal;
        @SerializedName("translateModel") float[] posModel;
        @SerializedName("overwriteConflict") Boolean overwriteConflict = null;

        // properties for SPacketPlayWith
        @SerializedName("followX") Boolean followX = null;
        @SerializedName("followY") Boolean followY = null;
        @SerializedName("followZ") Boolean followZ = null;
        @SerializedName("followYaw") Boolean followYaw = null;
        @SerializedName("followPitch") Boolean followPitch = null;
        @SerializedName("useHead") Boolean useHead = null;
        @SerializedName("useRender") Boolean useRender = null;
        @SerializedName("inheritYaw") Boolean inheritYaw = null;
        @SerializedName("inheritPitch") Boolean inheritPitch = null;
        @SerializedName("dynamicInput") float[] dynamics = null;

        boolean checkDefaults() {
            if (effect == null || lifespan == null || lifespan < 0) return false;

            if (skipFrame == null) skipFrame = 0;

            if (scale == null || scale.length < 3) scale = new float[]{1, 1, 1};
            if (rotateLocal == null || rotateLocal.length < 2) rotateLocal = new float[2];
            if (rotateModel == null || rotateModel.length < 2) rotateModel = new float[2];
            if (posLocal == null || posLocal.length < 3) posLocal = new float[3];
            if (posModel == null || posModel.length < 3) posModel = new float[3];

            if (overwriteConflict == null) overwriteConflict = false;
            if (followX == null) followX = false;
            if (followY == null) followY = false;
            if (followZ == null) followZ = false;
            if (followYaw == null) followYaw = false;
            if (followPitch == null) followPitch = false;

            if (useHead == null) useHead = false;
            if (useRender == null) useRender = false;

            if (inheritYaw == null) inheritYaw = true;
            if (inheritPitch == null) inheritPitch = true;

            if (dynamics == null) dynamics = new float[0];

            return true;
        }

        void extendsFrom(Pojo parent) {
            if (effect == null) effect = parent.effect;
            if (lifespan == null) lifespan = parent.lifespan;
            if (skipFrame == null) skipFrame = parent.skipFrame;

            if (scale == null || scale.length < 3) scale = parent.scale;
            if (rotateLocal == null || rotateLocal.length < 2) rotateLocal = parent.rotateLocal;
            if (rotateModel == null || rotateModel.length < 2) rotateModel = parent.rotateModel;
            if (posLocal == null || posLocal.length < 3) posLocal = parent.posLocal;
            if (posModel == null || posModel.length < 3) posModel = parent.posModel;

            if (overwriteConflict == null) overwriteConflict = parent.overwriteConflict;
            if (followX == null) followX = parent.followX;
            if (followY == null) followY = parent.followY;
            if (followZ == null) followZ = parent.followZ;
            if (followYaw == null) followYaw = parent.followYaw;
            if (followPitch == null) followPitch = parent.followPitch;

            if (useHead == null) useHead = parent.useHead;
            if (useRender == null) useRender = parent.useRender;

            if (inheritYaw == null) inheritYaw = parent.inheritYaw;
            if (inheritPitch == null) inheritPitch = parent.inheritPitch;

            if (dynamics == null) dynamics = parent.dynamics;
        }
    }

    private static class Entry extends EffectEntry {

        static final Entry DEFAULT = new Entry(null);

        final boolean valid;

        private Entry(Void unused) {

            this.valid = false;

            this.effect = null;
            this.lifespan = -1;
            this.followX = this.followY = this.followZ = this.followYaw = this.followPitch = false;
            this.inheritYaw = this.inheritPitch = true;
            this.useHead = this.useRender = false;
            this.overwrite = false;

            this.skipFrames = 0;
            this.dynamic = null;

            this.localPos = this.modelPos = new float[3];
            this.localRot = this.modelRot = new float[2];
            this.scale = new float[] {1, 1, 1};
        }

        Entry(Entry parent, Pojo pojo) {
            if (parent == null) parent = DEFAULT;

            this.effect = getOrDefault(pojo.effect, parent.effect);
            this.lifespan = getOrDefault(pojo.lifespan, parent.lifespan);
            this.valid = this.effect != null && this.lifespan > 0;

            if (!this.valid) return;

            this.skipFrames = getOrDefault(pojo.skipFrame, parent.skipFrames);

            this.overwrite = getOrDefault(pojo.overwriteConflict, parent.overwrite);

            this.followX = getOrDefault(pojo.followX, parent.followX);
            this.followY = getOrDefault(pojo.followY, parent.followY);
            this.followZ = getOrDefault(pojo.followZ, parent.followZ);
            this.followYaw = getOrDefault(pojo.followYaw, parent.followYaw);
            this.followPitch = getOrDefault(pojo.followPitch, parent.followPitch);

            this.inheritYaw = getOrDefault(pojo.inheritYaw, parent.inheritYaw);
            this.inheritPitch = getOrDefault(pojo.inheritPitch, parent.inheritPitch);

            this.useHead = getOrDefault(pojo.useHead, parent.useHead);
            this.useRender = getOrDefault(pojo.useRender, parent.useRender);

            this.localPos = getOrDefault(pojo.posLocal, parent.localPos).clone();
            this.modelPos = getOrDefault(pojo.posModel, parent.modelPos).clone();

            this.localRot = getOrDefault(pojo.rotateLocal, parent.localRot).clone();
            this.modelRot = getOrDefault(pojo.rotateModel, parent.modelRot).clone();

            this.scale = getOrDefault(pojo.scale, parent.scale).clone();

            this.dynamic = pojo.dynamics != null ? pojo.dynamics : parent.dynamic;
        }

        private static String getOrDefault(String a, String b) {
            return a == null ? b : a;
        }

        private static boolean getOrDefault(Boolean a, boolean b) {
            return a == null ? b : a;
        }

        private static int getOrDefault(Integer a, int b) {
            return a == null ? b : a;
        }

        private static float[] getOrDefault(float[] a, float[] b) {
            return a == null || a.length < b.length ? b : a;
        }
    }

    private static final Type type = new TypeToken<HashMap<String, Pojo>>() {}.getType();
    private final Map<String, Entry> map = new ConcurrentHashMap<>();
    private final File file;
    private CompletableFuture<Map<String, Entry>> future;
    private boolean available = false;

    EffectMap(File file) {
        this.file = file;
        reload(() -> {});
    }

    public void reload(Runnable runnable) {
        available = false;
        if (future != null && !future.isCancelled()) {
            future.join(); // wait here, and throw out any exception in necessary;
        }

        if (file.exists()) {
            future = CompletableFuture.supplyAsync(this::asyncLoad);
            future.thenRun(runnable);
        } else {
            future = CompletableFuture.completedFuture(Collections.emptyMap());
            future.thenRun(runnable);
        }
    }

    private Map<String, Entry> asyncLoad() {
        if (file.exists()) {
            Gson gson = new Gson();
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                Map<String, Pojo> tmp = gson.fromJson(reader, type);
                Map<String, Entry> baked = new HashMap<>();
                int size;
                do {
                    size = baked.size();
                    Iterator<Map.Entry<String, Pojo>> iterator = tmp.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Pojo> entry = iterator.next();
                        Pojo node = entry.getValue();
                        Entry parentNode = null;

                        if (node.parent == null || (parentNode = baked.get(node.parent)) != null) {

                            iterator.remove();

                            Entry self = new Entry(parentNode, node);
                            if (self.valid) baked.put(entry.getKey(), self);
                        }
                    }
                } while (size != baked.size() && !tmp.isEmpty());
                return baked;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Collections.emptyMap();
    }

    private void checkFuture() {
        if (available) return;

        Map<String, Entry> tmp = future.join();
        this.map.clear();
        this.map.putAll(tmp);

        this.available = true;
    }

    public Set<String> keySets() {
        checkFuture();
        return new HashSet<>(map.keySet());
    }

    public EffectEntry getEffect(String key) {
        return map.get(key);
    }

    public boolean isExist(String key) {
        checkFuture();
        return map.containsKey(key);
    }

//    public SPacketPlayWith createPlayWith(String key, String emitter, int entityId) {
//        checkFuture();
//
//        Pojo node = map.get(key);
//        if (node == null) return null;
//
//        SPacketPlayWith packet = new SPacketPlayWith(key, node.effect, emitter, node.lifespan, entityId);
//        return buildPacketFromNode(packet, node)
//                .markFollowX(node.followX)
//                .markFollowY(node.followY)
//                .markFollowZ(node.followZ)
//                .markFollowYaw(node.followYaw)
//                .markFollowPitch(node.followPitch)
//                .markUseHead(node.useHead)
//                .markUseRender(node.useRender)
//                .markInheritYaw(node.inheritYaw)
//                .markInheritPitch(node.inheritPitch);
//    }

//    public SPacketPlayWith createPlayWith(String key, String emitter, int entityId, String[] args) {
//        SPacketPlayWith play = createPlayWith(key, emitter, entityId);
//
//        CommandUtils.parse(play, args);
//        return play;
//    }

//    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z) {
//        checkFuture();
//
//        Pojo node = map.get(key);
//        if (node == null) return null;
//
//        SPacketPlayAt packet = new SPacketPlayAt(key, node.effect, emitter, node.lifespan, x, y, z);
//        return buildPacketFromNode(packet, node);
//    }

//    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z, double yaw, double pitch) {
//        checkFuture();
//
//        Pojo node = map.get(key);
//        if (node == null) return null;
//
//        SPacketPlayAt packet = new SPacketPlayAt(key, node.effect, emitter, node.lifespan, x, y, z, (float) yaw, (float) pitch);
//        buildPacketFromNode(packet, node);
//
//        return packet;
//    }

//    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z, float yaw, float pitch, String[] args) {
//        SPacketPlayAt play = createPlayAt(key, emitter, x, y, z, yaw, pitch);
//
//        CommandUtils.parse(play, args);
//        return play;
//    }

//    private <T extends SPacketPlayAbstract> T buildPacketFromNode(T packet, Pojo node) {
//        packet.skipFrame(node.skipFrame)
//                .markConflictOverwrite(node.overwriteConflict)
//                .scaleTo(node.scale[0], node.scale[1], node.scale[2])
//                .rotateLocalTo(node.rotateLocal[0], node.rotateLocal[1])
//                .translateLocalTo(node.posLocal[0], node.posLocal[1], node.posLocal[2])
//                .rotateModelTo(node.rotateModel[0], node.rotateModel[1])
//                .translateModelTo(node.posModel[0], node.posModel[1], node.posModel[2])
//                .setDynamics(node.dynamics);
//
//        return packet;
//    }

//    public SPacketStop createStop(String key) {
//        return createStop(key, null);
//    }

//    public SPacketStop createStop(String key, String emitter) {
//        checkFuture();
//
//        return new SPacketStop(key, emitter == null ? "*" : emitter);
//    }
}
