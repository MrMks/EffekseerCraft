package com.github.mrmks.mc.efscraft.common;

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

class EffectMap {

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
    }

    private static class Entry extends EffectEntry {

        static final Entry DEFAULT = new Entry();

        final boolean valid;

        private Entry() {
            this.valid = false;
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
            if (this.dynamic != null) this.dynamic = this.dynamic.clone();
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
    private static final Runnable EMPTY = () -> {};
    private final Map<String, Entry> map = new ConcurrentHashMap<>();
    private final File file;
    private CompletableFuture<Map<String, Entry>> future;
    private boolean available = false;

    EffectMap(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        reload(EMPTY);
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
                            if (self.valid)
                                baked.put(entry.getKey(), self);
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

    public EffectEntry get(String key) {
        return map.get(key);
    }

    public boolean isExist(String key) {
        checkFuture();
        return map.containsKey(key);
    }

}
