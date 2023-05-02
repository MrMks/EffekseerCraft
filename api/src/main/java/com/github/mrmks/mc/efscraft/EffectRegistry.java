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
        @SerializedName("effect") String effect = null;
        @SerializedName("lifespan") int lifespan = -1;
        @SerializedName("skipFrame") int skipFrame;
        @SerializedName("scale") float[] scale;
        @SerializedName("rotateLocal") float[] rotateLocal;
        @SerializedName("rotateModel") float[] rotateModel;
        @SerializedName("translateLocal") float[] posLocal;
        @SerializedName("translateModel") float[] posModel;
        @SerializedName("followX") boolean followX;
        @SerializedName("followY") boolean followY;
        @SerializedName("followZ") boolean followZ;
        @SerializedName("followYaw") boolean followYaw;
        @SerializedName("followPitch") boolean followPitch;
        @SerializedName("overwriteConflict") boolean overwriteConflict;

        boolean checkDefaults() {
            if (effect == null || lifespan < 0) return false;

            if (scale == null || scale.length < 3) scale = new float[3];
            if (rotateLocal == null || rotateLocal.length < 2) rotateLocal = new float[2];
            if (rotateModel == null || rotateModel.length < 2) rotateModel = new float[2];
            if (posLocal == null || posLocal.length < 3) posLocal = new float[3];
            if (posModel == null || posModel.length < 3) posModel = new float[3];

            return true;
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
                tmp.values().removeIf(it -> !it.checkDefaults());
                return tmp;
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

        SPacketPlayWith packet = new SPacketPlayWith(node.effect, emitter, node.lifespan, entityId);
        buildPacketFromNode(packet, node);

        if (node.followX) packet.markFollowX();
        if (node.followY) packet.markFollowY();
        if (node.followZ) packet.markFollowZ();
        if (node.followYaw) packet.markFollowYaw();
        if (node.followPitch) packet.markFollowPitch();

        return packet;
    }

    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayAt packet = new SPacketPlayAt(node.effect, emitter, node.lifespan, x, y, z);
        buildPacketFromNode(packet, node);

        return packet;
    }

    public SPacketPlayAt createPlayAt(String key, String emitter, double x, double y, double z, double yaw, double pitch) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayAt packet = new SPacketPlayAt(node.effect, emitter, node.lifespan, x, y, z, (float) yaw, (float) pitch);
        buildPacketFromNode(packet, node);

        return packet;
    }

    private void buildPacketFromNode(SPacketPlayAbstract packet, Node node) {
        if (node.skipFrame > 0) packet.skipFrame(node.skipFrame);
        if (node.overwriteConflict) packet.markConflictOverwrite();

        packet.scaleTo(node.scale[0], node.scale[1], node.scale[2]);
        packet.rotateLocalTo(node.rotateLocal[0], node.rotateLocal[1]);
        packet.translateLocalTo(node.posLocal[0], node.posLocal[1], node.posLocal[2]);
        packet.rotateModelTo(node.rotateModel[0], node.rotateModel[1]);
        packet.translateModelTo(node.posModel[0], node.posModel[1], node.posModel[2]);
    }

    public SPacketStop createStop(String key) {
        return createStop(key, null);
    }

    public SPacketStop createStop(String key, String emitter) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        return new SPacketStop(node.effect, emitter == null ? "" : emitter);
    }
}
