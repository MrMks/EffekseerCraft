package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EfsEffect;
import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.client.ResourceLoader;
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
import java.util.Locale;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ResourceManager extends ResourceLoader<IResourceManager, ResourceLocation> implements ISelectiveResourceReloadListener {

    enum ResourceEffect implements IResourceType { INSTANCE }

    private static final MethodHandle GET_DOMAIN_MANAGER;
    private static final MethodHandle GET_RESOURCE_PACK;
    private static final MethodHandle MAP_INVOKE_GET;
    private static final MethodHandle GET_PACK_FILE;
    private static final MethodHandle GET_PACK_ZIP;

    static {
        MethodHandle tmp;
        boolean deobf = FMLLaunchHandler.isDeobfuscatedEnvironment();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            Field field = SimpleReloadableResourceManager.class.getDeclaredField(deobf ? "domainResourceManagers" : "field_110548_a");
            field.setAccessible(true);
            tmp = lookup.unreflectGetter(field);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_DOMAIN_MANAGER = tmp;

        try {
            Field field= FallbackResourceManager.class.getDeclaredField(deobf ? "resourcePacks" : "field_110540_a");
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
            Field field = AbstractResourcePack.class.getDeclaredField(deobf ? "resourcePackFile" : "field_110597_b");
            field.setAccessible(true);
            tmp = lookup.unreflectGetter(field);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_PACK_FILE = tmp;

        try {
            Method method = FileResourcePack.class.getDeclaredMethod(deobf ? "getResourcePackZipFile" : "func_110599_c");
            method.setAccessible(true);
            tmp = lookup.unreflect(method);
        } catch (Throwable tr) {
            tr.printStackTrace();
            tmp = null;
        }
        GET_PACK_ZIP = tmp;
    }

    ResourceManager(ILogAdaptor logger) {
        super(logger);
    }

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
            }
        }
        return keys.stream().map(it -> it.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(ResourceEffect.INSTANCE)) {
            doClear();
            doLoad(resourceManager, searchPacks(resourceManager));
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

    EfsEffect get(String effectKey) {
        return effects.get(effectKey);
    }

    void cleanup() {
        doClear();
    }

}
