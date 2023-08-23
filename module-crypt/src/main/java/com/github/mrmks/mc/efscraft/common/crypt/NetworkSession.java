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

    private static void writePublicKey(PublicKey key, OutputStream output) {
        try {
            DataOutputStream stream = new DataOutputStream(output);
            writeBytes(key.getEncoded(), output);
            stream.writeUTF(key.getAlgorithm());
        } catch (IOException e) {
            // this should never happen;
        }
    }

    private static PublicKey readPublicKey(InputStream input) throws IOException {
        DataInputStream stream = new DataInputStream(input);
        byte[] bytes = readBytes(input);
        String alg = stream.readUTF();

        return pubKeyFromBytes(alg, bytes);
    }

    private static void writeBytes(byte[] data, OutputStream output) {
        try {
            new DataOutputStream(output).writeInt(data.length);
            output.write(data);
        } catch (IOException e) {
            // this should never happen;
        }
    }

    private static byte[] readBytes(InputStream input) throws IOException {
        int len = new DataInputStream(input).readInt();

        byte[] bytes = new byte[len];
        len = input.read(bytes);

        if (len != bytes.length)
            throw new IOException("un-match data size.");

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

    public static class Server extends Common {

        private KeyPair rsaPair;
        private byte[] rnd;
        public byte[] handshakeHello() {
            rsaPair = genRSAPair();
            SecureRandom rGen = new SecureRandom();
            rnd = new byte[rGen.nextInt(16) + 64];
            rGen.nextBytes(rnd);

            ByteArrayOutputStream data = new ByteArrayOutputStream();

            writePublicKey(rsaPair.getPublic(), data);
            writeBytes(rnd, data);

            return data.toByteArray();
        }

        public byte[] handshakeConfirm(byte[] input) throws IOException {
            return handshakeConfirm(new ByteArrayInputStream(input));
        }

        public byte[] handshakeConfirm(InputStream input) throws IOException {
            byte[] bytes = readBytes(input);
            bytes = decryptWithRSA(rsaPair.getPrivate(), bytes);

            input = new ByteArrayInputStream(bytes);
            bytes = readBytes(input);

            if (!Arrays.equals(hash(rsaPair.getPublic(), rnd), bytes))
                return null;

            PublicKey clientDHPub = readPublicKey(input);

            KeyPair pair = genDHPair();

            bytes = agreementDH(pair.getPrivate(), clientDHPub);
            generateCipher(bytes);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writePublicKey(pair.getPublic(), output);
            writeBytes(signWithRSA(rsaPair.getPrivate(), pair.getPublic().getEncoded()), output);

            rsaPair = null;
            rnd = bytes;

            return output.toByteArray();
        }

        public boolean handshakeDone(byte[] input) throws IOException {
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(input));

            int offset = stream.readInt();
            int len = stream.read();

            byte[] bytes = Arrays.copyOfRange(input, 5, input.length);
            boolean flag = Arrays.equals(bytes, exchangeConfirm(rnd, offset, len));

            rnd = null;

            return flag;
        }

        public boolean handshakeDone(InputStream input) throws IOException {
            DataInputStream stream = new DataInputStream(input);
            int offset = stream.readInt();
            int len = stream.readByte();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (int i = input.read(); i >= 0; i = input.read()) {
                os.write(i);
            }

            byte[] bytes = os.toByteArray();

            boolean flag = Arrays.equals(bytes, exchangeConfirm(rnd, offset, len));

            rnd = null;

            return flag;
        }

    }

    public static class Client extends Common {
        private PublicKey sKey;
        private KeyPair dhPair;

        public byte[] handshakeHello(byte[] input) throws IOException {
            return handshakeHello(new ByteArrayInputStream(input));
        }

        public byte[] handshakeHello(InputStream input) throws IOException {
            sKey = readPublicKey(input);
            byte[] rnd = readBytes(input);

            byte[] hash = hash(sKey, rnd);

            dhPair = genDHPair();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            writeBytes(hash, stream);
            writePublicKey(dhPair.getPublic(), stream);

            byte[] bytes = encryptWithRSA(sKey, stream.toByteArray());

            ByteArrayOutputStream data = new ByteArrayOutputStream();
            writeBytes(bytes, data);

            return data.toByteArray();
        }

        public byte[] handshakeConfirm(byte[] input) throws IOException {
            return handshakeConfirm(new ByteArrayInputStream(input));
        }

        public byte[] handshakeConfirm(InputStream input) throws IOException {
            PublicKey key = readPublicKey(input);
            byte[] bytes = readBytes(input);

            if (!verifyWithRSA(sKey, key.getEncoded(), bytes))
                return null;

            bytes = agreementDH(dhPair.getPrivate(), key);
            generateCipher(bytes);

            sKey = null;
            dhPair = null;

            ByteArrayOutputStream data = new ByteArrayOutputStream();
            SecureRandom random = new SecureRandom();
            int offset = random.nextInt(bytes.length - 64);
            int len = 48 + random.nextInt(16);

            DataOutputStream stream = new DataOutputStream(data);
            stream.writeInt(offset);
            stream.write(len);
            stream.write(exchangeConfirm(bytes, offset, len));

            return data.toByteArray();
        }
    }

}
