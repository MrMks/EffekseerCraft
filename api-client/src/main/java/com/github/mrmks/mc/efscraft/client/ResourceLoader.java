package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public abstract class ResourceLoader<RM, RL> {

    private interface LoadPredicate {
        boolean test(int i, InputStream stream) throws IOException;
    }

    private RL createLocation0(String effect, String resource) {
        return createLocation("efscraft", "effects/" + effect + "/" + resource);
    }

    protected final LogAdaptor logger;
    protected ResourceLoader(LogAdaptor adaptor) {
        this.logger = adaptor;
    }

    protected abstract RL createLocation(String key, String path);
    private boolean loadResource0(
            RM manager,
            String key,
            IntSupplier counter,
            IntFunction<String> pathGetter,
            LoadPredicate consumer) {

        int count = counter.getAsInt();
        String path;
        RL loc;
        for (int i = 0; i < count; i++) {
            path = pathGetter.apply(i).toLowerCase(Locale.ENGLISH);
            loc = createLocation0(key, path);

            try {
                InputStream stream = loadResource(manager, loc);
                if (!consumer.test(i, stream))
                    return false;
            } catch (IOException e) {
                String msg = "Unable to load resource file " + path + " of effect " + key;
                if (e instanceof FileNotFoundException) {
                    logger.logWarning(msg + ": " + e.getMessage());
                } else {
                    logger.logWarning(msg, e);
                }
                return false;
            }
        }

        return true;
    }

    protected abstract InputStream loadResource(RM resourceManager, RL resourceLocation) throws IOException;


    protected final Map<String, EfsEffect> effects = new HashMap<>();

    protected final void doLoad(RM resourceManager, Collection<String> keys) {
        for (String key : keys)
            doLoad(resourceManager, key);

        logger.logDebug("Loaded effects: ");
        Iterator<String> it = effects.keySet().iterator();
        while (it.hasNext()) {
            StringBuilder bd = new StringBuilder("    ");
            for (int i = 0; i < 5 && it.hasNext(); i++)
                bd.append(it.next()).append(", ");

            logger.logDebug(bd.toString());
        }
    }

    private void doLoad(RM resourceManager, String effectKey) {

        EfsEffect effect = new EfsEffect();

        try (InputStream stream = loadResource(resourceManager, createLocation0(effectKey, effectKey + ".efkefc"))) {
            if (!effect.load(stream, 1.0f, true)) {
                effect.delete();
                return;
            }
        } catch (IOException e) {
            String msg = "Unable to load effect " + effectKey;
            if (e instanceof FileNotFoundException) {
                logger.logWarning(msg + ": " + e.getMessage());
            } else {
                logger.logWarning(msg, e);
            }
            effect.delete();
            return;
        }

        boolean flag = true;
        for (EfsEffect.Texture texture : EfsEffect.Texture.values()) {
            flag = loadResource0(resourceManager, effectKey,
                    () -> effect.textureCount(texture),
                    i -> effect.getTexturePath(i, texture),
                    (i, in) -> effect.loadTexture(in, i, texture, true)
            );
            if (!flag) break;
        }

        flag = flag && loadResource0(resourceManager, effectKey, effect::curveCount, effect::getCurvePath,
                (i, stream) -> effect.loadCurve(stream, i, true));

        flag = flag && loadResource0(resourceManager, effectKey, effect::materialCount, effect::getMaterialPath,
                (i, stream) -> effect.loadMaterial(stream, i, true));

        flag = flag && loadResource0(resourceManager, effectKey, effect::modelCount, effect::getModelPath,
                (i, stream) -> effect.loadModel(stream, i, true));

        if (flag)
            effects.putIfAbsent(effectKey, effect);
        else
            effect.delete();
    }

    protected final void doClear() {
        Collection<EfsEffect> collection = new ArrayList<>(effects.values());
        effects.clear();
        collection.forEach(EfsEffect::delete);
    }

}
