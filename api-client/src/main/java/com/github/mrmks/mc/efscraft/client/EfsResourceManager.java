package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.efkseer4j.EfsEffect;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//import static com.github.mrmks.mc.efscraft.common.crypt.CryptUtils.digestWithSha256;

class EfsResourceManager {

    private interface InputStreamConsumer {
        boolean accept(int i, InputStream stream) throws IOException;
    }

    private final EfsClient<?, ?, ?> client;
    private final File folder;
    private List<Pack> packs;
    private final Map<String, EncryptedPack> digestToPack = new HashMap<>();
    private final Map<String, EfsEffect> effects = new HashMap<>();

    EfsResourceManager(EfsClient<?, ?, ?> client, File file) {
        this.client = client;
        this.folder = file;
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
                                packs.add(new ZipPack(file));
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

            for (Pack pack : packs) {
                if (pack instanceof EncryptedPack && !pack.isDecrypted()) {
                    EncryptedPack ep = (EncryptedPack) pack;
                    String digest64 = Base64.getEncoder().encodeToString(ep.getDigest());
                    digestToPack.put(digest64, ep);
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

        loadPacks();
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

    Set<String> encryptedDigests() {
        return new HashSet<>(digestToPack.keySet());
    }

    void receiveDecryptKey(Map<String, byte[]> keys) {
        keys.forEach((d, k) -> {
            EncryptedPack ep = digestToPack.get(d);
            if (ep != null) ep.setDecryptKey(k);
        });
    }

    private EfsEffect load(String key) {

        EfsEffect effect = null;

        boolean flag = false;

        try (InputStream stream = loadResource0("effects/" + key + "/" + key + ".efkefc")) {
            effect = new EfsEffect();
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

        EfsEffect fe = effect;

        for (EfsEffect.Texture texture : EfsEffect.Texture.values()) {
            flag = flag && loadResource0(
                    key,
                    () -> fe.textureCount(texture),
                    i -> fe.getTexturePath(i, texture),
                    (i, in) -> fe.loadTexture(in, i, texture, true)
            );
        }

        flag = flag && loadResource0(key, effect::curveCount, effect::getCurvePath, (i, in) -> fe.loadCurve(in, i, true));
        flag = flag && loadResource0(key, effect::materialCount, effect::getMaterialPath, (i, in) -> fe.loadMaterial(in, i, true));
        flag = flag && loadResource0(key, effect::modelCount, effect::getModelPath, (i, in) -> fe.loadModel(in, i, true));

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
        for (Pack pack : packs) {
            if (pack.isDecrypted() && pack.fileExist(path)) {
                try {
                    return pack.loadResource(path);
                } catch (IOException e) {
                    client.logger.logWarning("Unable to load a resource from resource pack: " + path + " in " + pack.getName(), e);
                    return null;
                }
            }
        }

        client.logger.logWarning("Unable to load resource from all packs: " + path);
        return null;
    }

    // internal classes
    private interface Pack {
        String getName();
        boolean isDecrypted();
        boolean fileExist(String path);
        InputStream loadResource(String path) throws IOException;
        void close() throws IOException;
    }

    public interface EncryptedPack extends Pack {
        byte[] getDigest();
        void setDecryptKey(byte[] key);
    }

    private static class ZipPack implements EncryptedPack {

        private final String name;
        private final ZipFile zipFile;
        private final byte[] digest;
        private final boolean blockExist;
        private byte[] decryptKey;

        private static byte[] digestWithSha256(InputStream stream) throws IOException {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                // this should never happen
                throw new RuntimeException(e);
            }

            int l; byte[] c = new byte[1024];
            while ((l = stream.read(c)) >= 0)
                digest.update(c, 0, l);

            return digest.digest();
        }

        ZipPack(File file) throws IOException {
            this.name = file.getName();
            this.zipFile = new ZipFile(file);
            try (InputStream stream = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
                digest = digestWithSha256(stream);
            }

            this.blockExist = fileExist(".block");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isDecrypted() {
            return !blockExist || decryptKey != null;
        }

        @Override
        public boolean fileExist(String path) {
            try {
                return zipFile.getEntry(path) != null;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public InputStream loadResource(String path) throws IOException {
            if (!isDecrypted()) throw new UnsupportedEncodingException(); // this should never happen;

            ZipEntry entry = zipFile.getEntry(path);
            InputStream stream = zipFile.getInputStream(entry);
            if (decryptKey != null) {
                return new InputStream() {

                    private final byte[] block = new byte[decryptKey.length];
                    private int len = 0, max = len;

                    @Override
                    public int read() throws IOException {

                        if (len >= max && max >= 0) {
                            max = stream.read(block);
                            for (int i = 0; i < max; i++)
                                block[i] ^= decryptKey[i];

                            len = 0;
                        }

                        return max < 0 ? -1 : (block[len ++] & 0xFF);
                    }
                };
            } else {
                return stream;
            }
        }

        @Override
        public void close() throws IOException {
            zipFile.close();
        }

        @Override
        public byte[] getDigest() {
            return Arrays.copyOf(digest, digest.length);
        }

        @Override
        public void setDecryptKey(byte[] key) {
            this.decryptKey = key;
        }
    }

    private static class FolderResourcePack implements Pack {

        File file;

        FolderResourcePack(File file) throws IOException {
            this.file = file;
            if (!(file.exists() && file.canRead()))
                throw new FileNotFoundException(file.getPath());
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public boolean isDecrypted() {
            return true;
        }

        @Override
        public boolean fileExist(String path) {
            return new File(this.file, path).exists();
        }

        @Override
        public InputStream loadResource(String path) throws IOException {
            File file = new File(this.file, path);
            return new BufferedInputStream(Files.newInputStream(file.toPath()));
        }

        @Override
        public void close() throws IOException {}
    }

}
