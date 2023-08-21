package com.github.mrmks.mc.efscraft.common.crypt;

import javax.crypto.Cipher;
import java.io.*;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Supplier;

import static com.github.mrmks.mc.efscraft.common.crypt.CryptUtils.*;

public class NetworkSession {

    private static void writePublicKey(PublicKey key, DataOutput output) {
        try {
            writeBytes(key.getEncoded(), output);
            output.writeUTF(key.getAlgorithm());
        } catch (IOException e) {
            // this should never happen;
        }
    }

    private static PublicKey readPublicKey(DataInput input) throws IOException {
        byte[] bytes = readBytes(input);
        String alg = input.readUTF();

        return pubKeyFromBytes(alg, bytes);
    }

    private static void writeBytes(byte[] data, DataOutput output) {
        try {
            output.writeInt(data.length);
            output.write(data);
        } catch (IOException e) {
            // this should never happen;
        }
    }

    private static byte[] readBytes(DataInput input) throws IOException {
        int len = input.readInt();
        byte[] bytes = new byte[len];

        input.readFully(bytes);

        return bytes;
    }

    public static abstract class Common {
        private Cipher aesEnc, aesDec;

        protected void generateCipher(byte[] ss) {
            this.aesEnc = genAesEncrypt(ss);
            this.aesDec = genAesDecrypt(ss);
        }

        public final byte[] encryptData(byte[] output) {
            return encryptWithAES(aesEnc, output);
        }

        public final byte[] decryptData(byte[] input) {
            return decryptWithAES(aesDec, input);
        }
    }

    public static class Server<DI extends DataInput, DO extends DataOutput> extends Common {

        private KeyPair rsaPair;
        private byte[] rnd;
        public DO handshakeHello(Supplier<? extends DO> supplier) {
            rsaPair = genRSAPair();
            SecureRandom rGen = new SecureRandom();
            rnd = new byte[rGen.nextInt(16) + 64];
            rGen.nextBytes(rnd);

            DO data = supplier.get();

            writePublicKey(rsaPair.getPublic(), data);
            writeBytes(rnd, data);

            return data;
        }

        public DO handshakeConfirm(DI input, Supplier<DO> supplier) throws IOException {
            byte[] bytes = readBytes(input);
            bytes = decryptWithRSA(rsaPair.getPrivate(), bytes);

            DataInput dataInput = new DataInputStream(new ByteArrayInputStream(bytes));
            bytes = readBytes(dataInput);

            if (!Arrays.equals(hash(rsaPair.getPublic(), rnd), bytes))
                return null;

            PublicKey clientDHPub = readPublicKey(dataInput);

            KeyPair pair = genDHPair();

            bytes = agreementDH(pair.getPrivate(), clientDHPub);
            generateCipher(bytes);

            DO output = supplier.get();
            writePublicKey(pair.getPublic(), output);
            writeBytes(signWithRSA(rsaPair.getPrivate(), pair.getPublic().getEncoded()), output);

            rsaPair = null;
            rnd = null;

            return output;
        }

    }

    public static class Client<DI extends DataInput, DO extends DataOutput> extends Common {
        private PublicKey sKey;
        private KeyPair dhPair;

        DO handshakeHello(DI input, Supplier<DO> supplier) throws IOException {
            sKey = readPublicKey(input);
            byte[] rnd = readBytes(input);

            byte[] hash = hash(sKey, rnd);

            dhPair = genDHPair();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutput output = new DataOutputStream(stream);

            writeBytes(hash, output);
            writePublicKey(dhPair.getPublic(), output);

            byte[] bytes = encryptWithRSA(sKey, stream.toByteArray());

            DO data = supplier.get();

            writeBytes(bytes, data);

            return data;
        }

        boolean handshakeConfirm(DI input) throws IOException {
            PublicKey key = readPublicKey(input);
            byte[] bytes = readBytes(input);

            if (!verifyWithRSA(sKey, key.getEncoded(), bytes))
                return false;

            bytes = agreementDH(dhPair.getPrivate(), key);
            generateCipher(bytes);

            sKey = null;
            dhPair = null;

            return true;
        }
    }

}
