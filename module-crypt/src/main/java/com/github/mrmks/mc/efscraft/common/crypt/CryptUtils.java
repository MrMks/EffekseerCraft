package com.github.mrmks.mc.efscraft.common.crypt;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CryptUtils {

    private static final String ALG_SIGN = "SHA256withRSA";
    private static final String ALG_AGREEMENT = "DiffieHellman";
    private static final String ALG_RSA = "RSA";
    private static final String ALG_RSA_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final String ALG_AES = "AES";
    private static final String ALG_AES_CIPHER = "AES/CBC/PKCS5Padding";
    private static final String ALG_SHA_256 = "SHA-256";

    private static final int KEY_SIZE_LONG = 2048;
    private static final int KET_SIZE_SHORT = 256;

    public static KeyPair genRSAPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALG_RSA);
            kpg.initialize(KEY_SIZE_LONG);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair genDHPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALG_AGREEMENT);
            kpg.initialize(KEY_SIZE_LONG);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] signWithRSA(PrivateKey key, byte[] data) {
        try {
            Signature signer = Signature.getInstance(ALG_SIGN);
            signer.initSign(key);
            signer.update(data);

            return signer.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyWithRSA(PublicKey key, byte[] data, byte[] signature) {
        try {
            Signature signer = Signature.getInstance(ALG_SIGN);
            signer.initVerify(key);
            signer.update(data);

            return signer.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptWithRSA(PublicKey key, byte[] data) {
        return cryptWithRSA(Cipher.ENCRYPT_MODE, key, data);
    }

    public static byte[] decryptWithRSA(PrivateKey key, byte[] data) {
        return cryptWithRSA(Cipher.DECRYPT_MODE, key, data);
    }

    private static byte[] cryptWithRSA(int mode, Key key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(ALG_RSA_CIPHER);
            cipher.init(mode, key);

            int size = KEY_SIZE_LONG / 8 - (mode == Cipher.ENCRYPT_MODE ? 11 : 0);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (int i = 0; i < data.length; i += size)
                stream.write(cipher.doFinal(data, i, Math.min(size, data.length - i)));

            return stream.toByteArray();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                 InvalidKeyException | IOException e) {
            // this should never happen;
            throw new RuntimeException(e);
        }
    }

    public static PublicKey pubKeyFromBytes(String alg, byte[] bytes) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        try {
            KeyFactory factory = KeyFactory.getInstance(alg);
            return factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] agreementDH(PrivateKey pri, PublicKey pub) {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance(ALG_AGREEMENT);
            agreement.init(pri);
            agreement.doPhase(pub, true);

            return agreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static Cipher genAesEncrypt(byte[] ss) {
        return genAesCipher(ss, Cipher.ENCRYPT_MODE);
    }

    public static Cipher genAesDecrypt(byte[] ss) {
        return genAesCipher(ss, Cipher.DECRYPT_MODE);
    }

    private static Cipher genAesCipher(byte[] ss, int mode) {
        try {
            MessageDigest dg = MessageDigest.getInstance(ALG_SHA_256);
            dg.update(ss, 0, ss.length - 16);

            Key keySpec = new SecretKeySpec(dg.digest(), ALG_AES);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ss, ss.length - 16, 16);

            Cipher cipher = Cipher.getInstance(ALG_AES_CIPHER);
            cipher.init(mode, keySpec, ivSpec);

            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptWithAES(Cipher cipher, byte[] data) {
        return cryptWithAES(cipher, data);
    }

    public static byte[] decryptWithAES(Cipher cipher, byte[] data) {
        return cryptWithAES(cipher, data);
    }

    private static byte[] cryptWithAES(Cipher cipher, byte[] data) {
        try {
            int bs = cipher.getBlockSize();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            for (int i = 0; i < data.length; i += bs)
                stream.write(cipher.update(data, i, Math.min(bs, data.length - i)));

            stream.write(cipher.doFinal());

            return stream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hash(PublicKey key, byte[] h2) {
        try {

            byte[] h0 = key.getEncoded();
            byte[] h1 = key.getAlgorithm().getBytes(StandardCharsets.UTF_8);

            MessageDigest digest = MessageDigest.getInstance(ALG_SHA_256);

            byte p = 0;
            for (int i = 0; i < h0.length; i++) {
                byte b = h0[i], c = b ^= p, d = h2[i % h2.length];

                if (((p >> 4) & (d << 2)) < 0) b &= d;
                if ((p ^ d) % 2 == 1) d = (byte) ~d;

                digest.update(b);
                digest.update(d);

                p = c;
            }

            byte[] m = digest.digest();

            digest.update(m);
            digest.update(h1);

            m = digest.digest();

            digest.update(m);
            digest.update(h2);

            m = digest.digest();

            return m;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
