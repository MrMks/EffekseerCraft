package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.client.ResourceLoader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ResourceManager extends ResourceLoader<IResourceManager, ResourceLocation> implements ISelectiveResourceReloadListener {

    enum ResourceEffect implements IResourceType { INSTANCE }

    ResourceManager(ILogAdaptor logger) {
        super(logger);
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(ResourceEffect.INSTANCE)) {
            doClear();
            Collection<String> keys = resourceManager.listResources("effects", it -> it.endsWith(".efkefc"))
                    .stream()
                    .filter(ResourceManager::filterResource)
                    .map(ResourceManager::mapResourceToKey)
                    .collect(Collectors.toSet());

            doLoad(resourceManager, keys);
        }
    }

    @Override
    protected ResourceLocation createLocation(String key, String path) {
        return new ResourceLocation(key, path);
    }

    @Override
    protected InputStream loadResource(IResourceManager resourceManager, ResourceLocation resourceLocation) throws IOException {
        return resourceManager.getResource(resourceLocation).getInputStream();
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

    private static String mapResourceToKey(ResourceLocation rl) {
        String path = rl.getPath();

        return path.substring(path.indexOf('/') + 1, path.lastIndexOf('/'));
    }

}
