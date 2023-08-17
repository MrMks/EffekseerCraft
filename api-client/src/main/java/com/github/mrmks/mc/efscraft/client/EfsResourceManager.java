package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class EfsResourceManager {

    private interface InputStreamConsumer {
        boolean accept(int i, InputStream stream) throws IOException;
    }

    private final EfsClient<?, ?, ?, ?> client;
    private final File folder;
    private List<ResourcePack> packs;
    private final Map<String, EfsEffect> effects = new HashMap<>();

    EfsResourceManager(EfsClient<?, ?, ?, ?> client, File file) {
        this.client = client;
        this.folder = file;
    }

    @Deprecated
    EfsResourceManager(EfsClient<?, ?, ?, ?> client) {
        this.client = client;
        this.folder = null;
    }

    private void loadPacks() {
        if (packs == null) {
            packs = new ArrayList<>();

            if (folder != null && folder.exists() && folder.isDirectory()) {
                File[] subs = folder.listFiles();

                if (subs != null) {
                    for (File file : subs) {
                        if (file.isFile() && file.getName().endsWith(".zip")) {
                            try {
                                packs.add(new ZipResourcePack(file));
                            } catch (IOException e) {
                                client.logger.logWarning("Unable to load resource pack: " + file, e);
                            }
                        } else if (file.isDirectory()) {
                            File pack = new File(file, ".pack");
                            if (pack.exists()) {
                                try {
                                    packs.add(new FolderResourcePack(file));
                                } catch (IOException e) {
                                    client.logger.logWarning("Unable to load resource pack folder: " + file, e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void onReload() {
        effects.values().forEach(EfsEffect::delete);
        effects.clear();

        if (packs != null) {
            packs.forEach(it -> {
                try {
                    it.close();
                } catch (IOException e) {
                    client.logger.logWarning("Unable to close a resource pack: " + it.getName(), e);
                }
            });
            packs.clear();
            packs = null;
        }
    }

    EfsEffect getOrLoad(String key) {
        EfsEffect effect = effects.get(key);
        if (effect == null) {

            effect = load(key);

            if (effect != null)
                effects.put(key, effect);

        }

        return effect;
    }

    private EfsEffect load(String key) {

        EfsEffect effect = new EfsEffect();

        boolean flag = false;

        try (InputStream stream = loadResource0("effects/" + key + "/" + key + ".efkefc")) {
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
            path = pathGetter.apply(i);

            try {
                InputStream stream = loadResource0("effects/" + key + "/" + path);
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

    private InputStream loadResource0(String path) {
        loadPacks();

        for (ResourcePack pack : packs) {
            try {
                Resource resource = pack.loadResource(path);
                if (resource.canDecrypt()) return resource.inputStream();
            } catch (FileNotFoundException e) {
                // do nothing
            } catch (IOException e) {
                client.logger.logWarning("Unable to load a resource from resource pack: " + path + " in " + pack.getName(), e);
            }
        }

        client.logger.logWarning("Unable to load resource from all packs: " + path);

        return null;
    }

    // internal classes

    private static abstract class Resource {

        final ResourcePack pack;
        Resource(ResourcePack pack) {
            this.pack = pack;
        }

        InputStream inputStream() throws IOException {

            if (pack.serverDecryptKey == null)
                return openStream();
            else {
                InputStream stream = openStream();
                return new InputStream() {

                    int i = 0;
                    int[] buffer;

                    private void fill() throws IOException {
                        if (buffer == null)
                            buffer = new int[pack.serverDecryptKey.length];

                        for (int i = 0; i < buffer.length; i++) {
                            int r = stream.read();

                            if (r == -1)
                                break;

                            int j = i % pack.decryptKey.length;
                            buffer[i] = (r ^ pack.serverDecryptKey[i] ^ pack.decryptKey[j]) & 0xFF;
                        }

                        i = 0;
                    }

                    @Override
                    public int read() throws IOException {

                        if (buffer == null || i >= buffer.length)
                            fill();

                        int r = buffer[i];
                        if (r != -1) i ++;
                        return r;
                    }
                };
            }
        }

        boolean canDecrypt() {
            return pack.decryptKey == null || pack.decryptKey.length == 0 || pack.serverDecryptKey != null;
        }

        abstract InputStream openStream() throws IOException;
    }

    private abstract static class ResourcePack {
        private byte[] decryptKey;
        private byte[] serverDecryptKey;
        final Set<String> error = new HashSet<>();

        void init() {
            try (InputStream stream = loadResource0("block")) {
                byte[] block = new byte[64];
                int i = 0, j;
                while ((j = stream.read()) != -1) {
                    if (i >= block.length) {
                        block = Arrays.copyOf(block, block.length + 64);
                    }
                    block[i ++] = (byte) j;
                }

                decryptKey = Arrays.copyOf(block, i);
            } catch (IOException e) {
                decryptKey = new byte[0];
            }
        }

        Resource loadResource(String path) throws IOException {
            if (error.contains(path))
                throw new FileNotFoundException(path);

            if (!fileExist(path)) {
                error.add(path);
                throw new FileNotFoundException(path);
            }

            return new Resource(this) {
                @Override
                InputStream openStream() throws IOException {
                    return loadResource0(path);
                }
            };
        }

        protected abstract String getName();
        protected abstract boolean fileExist(String path);
        protected abstract InputStream loadResource0(String path) throws IOException;

        protected abstract void close() throws IOException;
    }

    private static class ZipResourcePack extends ResourcePack {

        ZipFile zipFile;

        ZipResourcePack(File file) throws IOException {
            this.zipFile = new ZipFile(file);

            init();
        }

        @Override
        protected String getName() {
            return zipFile.getName();
        }

        @Override
        protected boolean fileExist(String path) {
            ZipEntry entry = zipFile.getEntry(path);
            return entry != null;
        }

        @Override
        protected InputStream loadResource0(String path) throws IOException {
            ZipEntry entry = zipFile.getEntry(path);
            if (entry == null) throw new FileNotFoundException(path);
            return zipFile.getInputStream(entry);
        }

        @Override
        protected void close() throws IOException {
            zipFile.close();
        }
    }

    private static class FolderResourcePack extends ResourcePack {

        File file;

        FolderResourcePack(File file) throws IOException {
            this.file = file;
            if (!(file.exists() && file.canRead()))
                throw new FileNotFoundException(file.getPath());

            init();
        }

        @Override
        protected String getName() {
            return file.getName();
        }

        @Override
        protected boolean fileExist(String path) {
            return new File(this.file, path).exists();
        }

        @Override
        protected InputStream loadResource0(String path) throws IOException {
            File file = new File(this.file, path);
            return new BufferedInputStream(Files.newInputStream(file.toPath()));
        }

        @Override
        protected void close() throws IOException {}
    }

}
