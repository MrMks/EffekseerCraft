package com.github.mrmks.mc.efscraft.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

class EfsSecretStore {
    private File file;
    private final Map<String, byte[]> secrets = new HashMap<>();

    void reload(File file) {
        this.file = file;
        reload();
    }

    Map<String, byte[]> mapTo(String[] ds) {
        if (ds.length == 0 || secrets.isEmpty()) return Collections.emptyMap();

        Map<String, byte[]> map = new HashMap<>();
        for (String d : ds) {
            byte[] key = secrets.get(d);
            if (key != null)
                map.put(d, key);
        }

        return map;
    }

    void reload() {
        secrets.clear();
        if (file.exists() && file.isFile() && file.canRead()) {
            try (InputStream stream = new BufferedInputStream(Files.newInputStream(file.toPath(), StandardOpenOption.READ))) {
                Gson gson = new Gson();
                JsonObject obj = gson.fromJson(new InputStreamReader(stream), JsonObject.class);

                Map<String, byte[]> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    String k = entry.getKey();
                    JsonElement je = entry.getValue();
                    if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()) {
                        byte[] v = Base64.getDecoder().decode(je.getAsString());

                        map.put(k, v);
                    }
                }

                secrets.putAll(map);
            } catch (IOException e) {
                // do nothing
            }
        }
    }
}
