package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.server.registry.PojoUpdate;
import com.github.mrmks.mc.efscraft.server.registry.PojoV1;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class ServerRegistryMap {

    private static final int CURRENT_VER = 1;

    private static class Entry extends ServerRegistry {

        static final Entry DEFAULT = new Entry();

        final boolean valid;

        private Entry() {
            this.valid = false;
            this.followArgs = new FollowArguments();
        }

        Entry(Entry parent, PojoV1 pojo) {
            if (parent == null) parent = DEFAULT;

            this.effect = getOrDefault(pojo.effect, parent.effect);
            this.lifespan = getOrDefault(pojo.lifespan, parent.lifespan);
            this.valid = this.effect != null && this.lifespan > 0;

            if (!this.valid) return;

            this.skipFrames = getOrDefault(pojo.skipFrame, parent.skipFrames);

            this.overwrite = getOrDefault(pojo.overwriteConflict, parent.overwrite);

            if (pojo.followArgs != null) {
                this.followArgs.followX = getOrDefault(pojo.followArgs.followX, parent.followArgs.followX);
                this.followArgs.followY = getOrDefault(pojo.followArgs.followY, parent.followArgs.followY);
                this.followArgs.followZ = getOrDefault(pojo.followArgs.followZ, parent.followArgs.followZ);
                this.followArgs.followYaw = getOrDefault(pojo.followArgs.followYaw, parent.followArgs.followYaw);
                this.followArgs.followPitch = getOrDefault(pojo.followArgs.followPitch, parent.followArgs.followPitch);

                this.followArgs.baseOnCurrentYaw = getOrDefault(pojo.followArgs.inheritYaw, parent.followArgs.baseOnCurrentYaw);
                this.followArgs.baseOnCurrentPitch = getOrDefault(pojo.followArgs.inheritPitch, parent.followArgs.baseOnCurrentPitch);

                this.followArgs.directionFromHead = getOrDefault(pojo.followArgs.useHead, parent.followArgs.directionFromHead);
                this.followArgs.directionFromBody = getOrDefault(pojo.followArgs.useRender, parent.followArgs.directionFromBody);
            }

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

    private static final Runnable EMPTY = () -> {};
    private final Map<String, Entry> map = new ConcurrentHashMap<>();
    private File[] files;
    private CompletableFuture<Map<String, Entry>> future;
    private boolean available = false;

    ServerRegistryMap(File... file) {
        this.files = file;
        reload();
    }

    ServerRegistryMap(List<File> file) {
        this.files = file.toArray(new File[0]);
        reload();
    }

    ServerRegistryMap() {}

    public void updateFiles(List<File> files) {
        this.files = files.toArray(new File[0]);
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

        future = CompletableFuture.supplyAsync(this::asyncLoad);
        future.thenRun(runnable);
    }

    private Map<String, Entry> asyncLoad0(File file) {
        if (file != null && file.exists() && file.canRead() && file.canWrite()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject basePojo;
            JsonObject content;

            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                basePojo = gson.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }

            int version = basePojo.has("cfgVersion") ? basePojo.get("cfgVersion").getAsInt() : 0;
            if (version == 0) {
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    content = gson.fromJson(reader, JsonObject.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    return Collections.emptyMap();
                }
            } else {
                content = basePojo.get("content").getAsJsonObject();
            }

            Map<String, PojoV1> tmp = PojoUpdate.update(PojoV1.class, gson, content, version);

            if (tmp != null) {
                try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                    JsonObject writeObj = new JsonObject();
                    writeObj.add("cfgVersion", new JsonPrimitive(CURRENT_VER));
                    writeObj.add("content", gson.toJsonTree(tmp));
                    gson.toJson(writeObj, writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Map<String, Entry> baked = new HashMap<>();
                int size;
                do {
                    size = baked.size();
                    Iterator<Map.Entry<String, PojoV1>> iterator = tmp.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, PojoV1> entry = iterator.next();
                        PojoV1 node = entry.getValue();
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
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Entry> asyncLoad() {

        if (files == null)
            return Collections.emptyMap();

        Map<String, Entry> baked = new HashMap<>();
        for (File file : files)
            baked.putAll(asyncLoad0(file));

        return baked.isEmpty() ? Collections.emptyMap() : baked;
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

    public ServerRegistry get(String key) {
        return map.get(key);
    }

    public boolean isExist(String key) {
        checkFuture();
        return map.containsKey(key);
    }

}
