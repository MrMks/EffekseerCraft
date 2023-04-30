package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EfsEffect;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ResourceManager implements ISelectiveResourceReloadListener {

    enum ResourceEffect implements IResourceType { INSTANCE }

    private static class ResourceLocator extends ResourceLocation {
        private final String path;
        public ResourceLocator(String pathIn) {
            super("efscraft", pathIn);
            this.path = pathIn;
        }

        @Override @Nonnull
        public String getPath() {
            return path;
        }

        @Override @Nonnull
        public String toString() {
            return namespace + ":" + path;
        }

        @Override
        public int hashCode() {
            return 31 * namespace.hashCode() + path.hashCode();
        }

        @Override
        public boolean equals(Object p_equals_1_) {
            if (p_equals_1_ == this) return true;
            else if (!(p_equals_1_ instanceof ResourceLocation)) return true;
            else {
                ResourceLocation rl = (ResourceLocation) p_equals_1_;

                return this.namespace.equals(rl.getNamespace()) && this.path.equals(rl.getPath());
            }
        }

        @Override
        public int compareTo(ResourceLocation p_compareTo_1_) {
            int i = namespace.compareTo(p_compareTo_1_.getNamespace());

            if (i == 0) {
                i = path.compareTo(p_compareTo_1_.getPath());
            }

            return i;
        }
    }

    private static ResourceLocation locEffect(String key) {
        return new ResourceLocator(String.format("effects/%s/%s.efkefc", key, key));
    }

    private static ResourceLocation locResources(String key, String path) {
        return new ResourceLocator(String.format("effects/%s/%s", key, path));
    }

    private static final MethodHandle GET_DOMAIN_MANAGER;
    private static final MethodHandle GET_RESOURCE_PACK;
    private static final MethodHandle MAP_INVOKE_GET;
    private static final MethodHandle GET_PACK_FILE;
    private static final MethodHandle GET_PACK_ZIP;

    static {
        MethodHandle tmp = null;
        boolean deobf = FMLLaunchHandler.isDeobfuscatedEnvironment();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            Field field = SimpleReloadableResourceManager.class.getDeclaredField(deobf ? "domainResourceManagers" : "");
            field.setAccessible(true);
            tmp = lookup.unreflectGetter(field);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_DOMAIN_MANAGER = tmp;

        try {
            Field field= FallbackResourceManager.class.getDeclaredField(deobf ? "resourcePacks" : "");
            field.setAccessible(true);
            tmp = lookup.unreflectGetter(field);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_RESOURCE_PACK = tmp;

        try {
            tmp = lookup.findVirtual(Map.class, "get", MethodType.methodType(Object.class, Object.class));
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        MAP_INVOKE_GET = tmp;

        try {
            Field field = AbstractResourcePack.class.getDeclaredField("resourcePackFile");
            field.setAccessible(true);
            tmp = lookup.unreflectGetter(field);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_PACK_FILE = tmp;

        try {
            Method method = FileResourcePack.class.getDeclaredMethod("getResourcePackZipFile");
            method.setAccessible(true);
            tmp = lookup.unreflect(method);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_PACK_ZIP = tmp;
    }

    private final Map<String, EfsEffect> effects = new HashMap<>();

    private Set<String> searchPacks(IResourceManager rm) {

        if (GET_DOMAIN_MANAGER == null || GET_RESOURCE_PACK == null || GET_PACK_FILE == null || MAP_INVOKE_GET == null)
            return Collections.emptySet();

        List<?> packs;
        try {
            packs = (List<?>) GET_RESOURCE_PACK.invoke(MAP_INVOKE_GET.invoke(GET_DOMAIN_MANAGER.invoke(rm), "efscraft"));
        } catch (Throwable tr) {
            tr.printStackTrace();
            return Collections.emptySet();
        }

        Set<String> keys = new HashSet<>();
        String searchPath = "assets/efscraft/effects/";

        for (Object obj : packs) {
            if (obj instanceof FolderResourcePack) {
                File file;
                try {
                    file = (File) GET_PACK_FILE.invoke(obj);
                } catch (Throwable tr) {
                    tr.printStackTrace();
                    continue;
                }

                file = new File(file, searchPath.replace('/', File.separatorChar));

                File[] sub = file.listFiles(it -> it.isDirectory() && it.exists());
                if (sub != null) for (File s : sub) keys.add(s.getName());
            } else if (obj instanceof FileResourcePack) {
                ZipFile file;
                try {
                    file = (ZipFile) GET_PACK_ZIP.invoke(obj);
                } catch (Throwable tr) {
                    tr.printStackTrace();
                    continue;
                }

                file.stream().filter(ZipEntry::isDirectory).map(ZipEntry::getName)
                        .filter(it -> it.startsWith(searchPath) && it.indexOf('/', searchPath.length()) == it.lastIndexOf('/'))
                        .forEach(it -> keys.add(it.substring(searchPath.length(), it.length() - 1)));
            } else {
                // unsupported resource pack format;
                continue;
            }
        }
        return keys;
    }

    private void doReload(IResourceManager resourceManager) {

        effects.clear();

        Set<String> registry = searchPacks(resourceManager);

        for (String key : registry) {
            // package is preferred;
            IResource resource;
            try {
                resource = resourceManager.getResource(locEffect(key));
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            EfsEffect effect = new EfsEffect();
            if (!effect.load(resource.getInputStream(), 1, true)) continue;

            boolean flag = true;
            for (EfsEffect.Texture texture : EfsEffect.Texture.values()) {
                flag = loadResource(resourceManager, key,
                        () -> effect.textureCount(texture),
                        i -> effect.getTexturePath(i, texture),
                        (i, in) -> effect.loadTexture(in, i, texture, true)
                );
                if (!flag) break;
            }
            if (!flag) continue;

            flag = loadResource(resourceManager, key, effect::curveCount, effect::getCurvePath,
                    (i, stream) -> effect.loadCurve(stream, i, true));
            if (!flag) continue;

            flag = loadResource(resourceManager, key, effect::materialCount, effect::getMaterialPath,
                    (i, stream) -> effect.loadMaterial(stream, i, true));
            if (!flag) continue;

            flag = loadResource(resourceManager, key, effect::modelCount, effect::getModelPath,
                    (i, stream) -> effect.loadModel(stream, i, true));
            if (!flag) continue;

            effects.putIfAbsent(key, effect);
        }
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(ResourceEffect.INSTANCE))
            doReload(resourceManager);
    }

    EfsEffect get(String effectKey) {
        return effects.get(effectKey);
    }

    private static boolean loadResource(
            IResourceManager manager,
            String key,
            IntSupplier counter,
            IntFunction<String> pathGetter,
            LoadPredicate consumer) {

        int count = counter.getAsInt();
        String path;
        IResource resource;
        ResourceLocation loc;
        for (int i = 0; i < count; i++) {
            path = pathGetter.apply(i);
            loc = locResources(key, path);
            try {
                resource = manager.getResource(loc);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            if (!consumer.test(i, resource.getInputStream())) {
                return false;
            }
        }

        return true;
    }

    private interface LoadPredicate {
        boolean test(int i, InputStream stream);
    }
}
