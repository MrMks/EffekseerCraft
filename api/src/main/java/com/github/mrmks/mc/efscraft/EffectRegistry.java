package com.github.mrmks.mc.efscraft;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EffectRegistry {
    public static class Node {
        @SerializedName("effect") String effect = null;
        @SerializedName("emitter") String emitter = null;
        @SerializedName("lifespan") int lifespan = -1;
        @SerializedName("skipframe") int skipFrame;
        @SerializedName("scale") float[] scale;
        @SerializedName("rotateLocal") float[] rotateLocal;
        @SerializedName("rotateModel") float[] rotateModel;
        @SerializedName("translate") float[] translate;
        @SerializedName("followX") boolean followX;
        @SerializedName("followY") boolean followY;
        @SerializedName("followZ") boolean followZ;
        @SerializedName("followYaw") boolean followYaw;
        @SerializedName("followPitch") boolean followPitch;
        @SerializedName("overwriteConflict") boolean overwriteConflict;

        boolean checkDefaults() {
            if (effect == null || emitter == null || lifespan < 0) return false;

            if (scale == null || scale.length < 3) scale = new float[3];
            if (rotateLocal == null || rotateLocal.length < 2) rotateLocal = new float[2];
            if (rotateModel == null || rotateModel.length < 2) rotateModel = new float[2];
            if (translate == null || translate.length < 3) translate = new float[3];

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

    public boolean isExist(String key) {
        return map.containsKey(key);
    }

    public SPacketPlayWith createPlayWith(String key, UUID uuid) {
        checkFuture();

        Node node = map.get(key);
        if (node == null) return null;

        SPacketPlayWith packet = new SPacketPlayWith(node.effect, node.emitter, node.lifespan, uuid);
        if (node.skipFrame > 0) packet.skipFrame(node.skipFrame);

        if (node.followX) packet.markFollowX();
        if (node.followY) packet.markFollowX();
        if (node.followZ) packet.markFollowZ();
        if (node.followYaw) packet.markFollowYaw();
        if (node.followPitch) packet.markFollowPitch();
        if (node.overwriteConflict) packet.markConflictOverwrite();

        packet.translate(node.translate[0], node.translate[1], node.translate[2]);
        packet.rotateLocal(node.rotateLocal[0], node.rotateLocal[1]);
        packet.rotateModel(node.rotateModel[0], node.rotateModel[1]);
        packet.scale(node.scale[0], node.scale[1], node.scale[2]);
        return packet;
    }

    public SPacketPlayAt createPlayAt(String key, double x, double y, double z) {
        checkFuture();

        return null; // todo
    }

    public SPacketStop createStop(String key) {
        checkFuture();

        return null; // todo;
    }
}
