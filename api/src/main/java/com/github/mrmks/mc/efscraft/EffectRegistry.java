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

public class EffectRegistry {
    public static class Node {
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

        void extendsFrom(Node parent) {
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

    private static final Type type = new TypeToken<HashMap<String, Node>>() {}.getType();

    private final Map<String, Node> map = new ConcurrentHashMap<>();
    private final File file;
    private CompletableFuture<Map<String, Node>> future;
    private boolean available = false;

    public EffectRegistry(File file) {
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

    private Map<String, Node> asyncLoad() {
        if (file.exists()) {
            Gson gson = new Gson();
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                Map<String, Node> tmp = gson.fromJson(reader, type);
                Map<String, Node> baked = new HashMap<>();
                int size;
                do {
                    size = baked.size();
                    Iterator<Map.Entry<String, Node>> iterator = tmp.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Node> entry = iterator.next();
                        Node node = entry.getValue(), parentNode = null;

                        if (node.parent == null || (parentNode = baked.get(node.parent)) != null) {
                            if (parentNode != null) node.extendsFrom(parentNode);
                            if (node.checkDefaults()) baked.put(entry.getKey(), node);
                            iterator.remove();
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

        Map<String, Node> tmp = future.join();
        this.map.putAll(tmp);

        this.available = true;
    }

    public Set<String> keySets() {
        checkFuture();
        return new HashSet<>(map.keySet());
    }

    public boolean isExist(String key) {
        checkFuture();
        return map.containsKey(key);
    }

    public SPacketPlayWith createPlayWith(String key, String emitter, int entityId) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayWith packet = new SPacketPlayWith(key, node.effect, emitter, node.lifespan, entityId);
        return buildPacketFromNode(packet, node)
                .markFollowX(node.followX)
                .markFollowY(node.followY)
                .markFollowZ(node.followZ)
                .markFollowYaw(node.followYaw)
                .markFollowPitch(node.followPitch)
                .markUseHead(node.useHead)
                .markUseRender(node.useRender)
                .markInheritYaw(node.inheritYaw)
                .markInheritPitch(node.inheritPitch);
    }

    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayAt packet = new SPacketPlayAt(key, node.effect, emitter, node.lifespan, x, y, z);
        return buildPacketFromNode(packet, node);
    }

    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z, double yaw, double pitch) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayAt packet = new SPacketPlayAt(key, node.effect, emitter, node.lifespan, x, y, z, (float) yaw, (float) pitch);
        buildPacketFromNode(packet, node);

        return packet;
    }

    private <T extends SPacketPlayAbstract> T buildPacketFromNode(T packet, Node node) {
        packet.skipFrame(node.skipFrame)
                .markConflictOverwrite(node.overwriteConflict)
                .scaleTo(node.scale[0], node.scale[1], node.scale[2])
                .rotateLocalTo(node.rotateLocal[0], node.rotateLocal[1])
                .translateLocalTo(node.posLocal[0], node.posLocal[1], node.posLocal[2])
                .rotateModelTo(node.rotateModel[0], node.rotateModel[1])
                .translateModelTo(node.posModel[0], node.posModel[1], node.posModel[2])
                .setDynamics(node.dynamics);

        return packet;
    }

    public SPacketStop createStop(String key) {
        return createStop(key, null);
    }

    public SPacketStop createStop(String key, String emitter) {
        checkFuture();

        return new SPacketStop(key, emitter == null ? "*" : emitter);
    }
}
