package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.mc.efscraft.client.ResourceLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

public class ResourceManager extends ResourceLoader<IResourceManager, ResourceLocation> implements ISelectiveResourceReloadListener {

    enum ResourceEffect implements IResourceType { INSTANCE }

    private final Logger logger;
    ResourceManager(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(ResourceEffect.INSTANCE)) {
            doClear();
            resourceManager.listResources("effects", it -> it.endsWith(".efkefc"))
                    .stream()
                    .filter(ResourceManager::filterResource)
                    .forEach(it -> loadEffect(resourceManager, it));
        }
    }

    @Override
    protected ResourceLocation createLocation(String key, String path) {
        return new ResourceLocation(key, path);
    }

    @Override
    protected void logException(String msg, Throwable tr) {
        if (tr == null)
            logger.error(msg);
        else
            logger.error(msg, tr);
    }

    @Override
    protected InputStream loadResource(IResourceManager resourceManager, ResourceLocation resourceLocation) throws IOException {
        return resourceManager.getResource(resourceLocation).getInputStream();
    }

    private void loadEffect(IResourceManager resourceManager, ResourceLocation rl) {
        String path = rl.getPath();
        String key = path.substring(path.indexOf('/') + 1, path.lastIndexOf('/'));

        doLoad(resourceManager, key);
    }

    EfsEffect get(String key) {
        return effects.get(key);
    }

    void cleanup() {
        doClear();
    }

    private static boolean filterResource(ResourceLocation rl) {
        if (rl == null) return false;
        if (!rl.getNamespace().equals("efscraft")) return false;

        String path = rl.getPath();
        if (!path.startsWith("effects/") || !path.endsWith(".efkefc")) return false;

        int i0 = path.indexOf('/');
        int i1 = path.lastIndexOf('/');
        int i2 = path.indexOf('/', i0 + 1);

        if (i0 == i1 || i1 != i2) return false;

        String folder = path.substring(i0 + 1, i1), file = path.substring(i1 + 1, path.length() - 7);

        return folder.equals(file);
    }

}
