package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import static com.github.mrmks.mc.efscraft.common.Constants.RESOURCE_NAMESPACE;

public class EfsResourceManager {

    private interface InputStreamConsumer {
        boolean accept(int i, InputStream stream) throws IOException;
    }

    private final EfsClient<?, ?, ?, ?> client;

    EfsResourceManager(EfsClient<?, ?, ?, ?> client) {
        this.client = client;
    }

    private Map<String, EfsEffect> effects = new HashMap<>();
    private Set<String> errorLoaded = new HashSet<>();


    void onReload() {
        effects.values().forEach(EfsEffect::delete);
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

        EfsEffect effect = new EfsEffect();

        boolean flag = false;

        try (InputStream stream = client.adaptor.loadResource(RESOURCE_NAMESPACE, key, key + ".efkefc")) {
            if (!(flag = effect.load(stream, 1, false))) {
                effect.delete();
            }
        } catch (IOException e) {
            String msg = "Unable to load effect " + key;
            if (e instanceof FileNotFoundException) {
                client.logger.logWarning(msg + ": " + e.getMessage());
            } else {
                client.logger.logWarning(msg, e);
            }
        }

        for (EfsEffect.Texture texture : EfsEffect.Texture.values()) {
            flag = flag && loadResource0(
                    key,
                    () -> effect.textureCount(texture),
                    i -> effect.getTexturePath(i, texture),
                    (i, in) -> effect.loadTexture(in, i, texture, true)
            );
        }

        flag = flag && loadResource0(key, effect::curveCount, effect::getCurvePath, (i, in) -> effect.loadCurve(in, i, true));
        flag = flag && loadResource0(key, effect::materialCount, effect::getMaterialPath, (i, in) -> effect.loadMaterial(in, i, true));
        flag = flag && loadResource0(key, effect::modelCount, effect::getModelPath, (i, in) -> effect.loadModel(in, i, true));

        if (!flag) {
            effect.delete();
        }

        return flag ? effect : null;
    }

    private boolean loadResource0(
            String key,
            IntSupplier counter,
            IntFunction<String> pathGetter,
            InputStreamConsumer consumer) {

        int count = counter.getAsInt();
        String path;
        for (int i = 0; i < count; i++) {
            path = pathGetter.apply(i).toLowerCase(Locale.ENGLISH);

            try {
                InputStream stream = client.adaptor.loadResource(RESOURCE_NAMESPACE, key, path);
                if (!consumer.accept(i, stream))
                    return false;
            } catch (IOException e) {
                String msg = "Unable to load resource file " + path + " of effect " + key;
                if (e instanceof FileNotFoundException) {
                    client.logger.logWarning(msg + ": " + e.getMessage());
                } else {
                    client.logger.logWarning(msg, e);
                }
                return false;
            }
        }

        return true;
    }

}
