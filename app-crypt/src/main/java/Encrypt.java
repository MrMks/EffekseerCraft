import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Encrypt {

    public static void main(String[] args) throws IOException {
        // args[0]: src file

        if (args.length < 1) {
            System.out.println("required input args: src");
            return;
        }

        File src = new File(args[0]);
        if (args.length > 1)
            decrypt(src, args[1]);
        else
            encrypt(src);
    }

    private static void encrypt(File src) throws IOException {
        if (!src.isFile() || !src.canRead() || !src.getName().endsWith(".zip")) {
            System.out.println("unreadable src file: " + src);
            return;
        }

        String name = src.getName();
        name = name.substring(0, name.length() - 4);

        // generate a random secret key;
        SecureRandom random = new SecureRandom();
        byte[] keys = new byte[48 + random.nextInt(16)];
        random.nextBytes(keys);

        File dst;

        dst = new File(src.getParent(), name + "-encrypt.zip");
        xorZipFile(src, dst, keys, true);
        String digest = digestFile(dst);

        // compute the digest
        dst = new File(src.getParent(), name + "-encrypt.json");
        {
            OutputStream stream = Files.newOutputStream(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Writer writer = new OutputStreamWriter(new BufferedOutputStream(stream), StandardCharsets.UTF_8);
            String k = Base64.getEncoder().encodeToString(keys);

            writer.write(String.format("{\"%s\": \"%s\"}", digest, k));
            writer.flush();
            writer.close();
        }
    }

    private static void decrypt(File src, String dec) throws IOException {
        if (!(src.exists() && src.isFile() && src.canRead() && src.getName().endsWith(".zip"))) {
            System.out.println("Unable to decrypt the src file: " + src);
            return;
        }

        String name = src.getName();
        name = name.substring(0, name.length() - 4);
        byte[] key = Base64.getDecoder().decode(dec);
        File dst = new File(src.getParent(), name + "-decrypt.zip");

        xorZipFile(src, dst, key, false);
    }

    private static String digestFile(File src) throws IOException {
        byte[] digest;
        try {
            MessageDigest digestGen = MessageDigest.getInstance("SHA-256");
            BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(src.toPath(), StandardOpenOption.READ));

            byte[] cache = new byte[1024];
            int l;
            while ((l = stream.read(cache)) >= 0) {
                digestGen.update(cache, 0, l);
            }

            digest = digestGen.digest();
        } catch (NoSuchAlgorithmException e) {
            // should never happen
            return null;
        }

        return Base64.getEncoder().encodeToString(digest);
    }

    private static void xorZipFile(File src, File dst, byte[] keys, boolean addBlock) throws IOException {
        ZipFile zipFile = new ZipFile(src);
        ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(dst.toPath(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE));

        if (addBlock)
            zipOutputStream.putNextEntry(new ZipEntry(".block"));

        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();

            if (!addBlock && entry.getName().equals(".block")) continue;
            zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
            InputStream stream = zipFile.getInputStream(entry);
            int i = 0, l;
            while ((l = stream.read()) != -1) {
                if (i >= keys.length) i = 0;
                zipOutputStream.write(l ^ keys[i++]);
            }
        }

        zipOutputStream.finish();
        zipOutputStream.flush();

        zipFile.close();
    }
}
