package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EfsResourceManager {

    private final EfsClient<?, ?, ?, ?> client;

    EfsResourceManager(EfsClient<?, ?, ?, ?> client) {
        this.client = client;
    }

    private Map<String, EfsEffect> effects = new HashMap<>();
    private Set<String> errorLoaded = new HashSet<>();


    void onReload() {
        effects.clear();
        errorLoaded.clear();
    }

    EfsEffect getOrLoad(String key) {
        EfsEffect effect = effects.get(key);
        if (effect == null) {
            if (errorLoaded.contains(key)) {
                return null;
            }

            effect = load(key);

            if (effect == null)
                errorLoaded.add(key);
            else
                effects.put(key, effect);

        }

        return effect;
    }

    private EfsEffect load(String key) {
        // todo
        return null;
    }

}
