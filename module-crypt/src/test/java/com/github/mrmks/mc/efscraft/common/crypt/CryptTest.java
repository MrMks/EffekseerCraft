package com.github.mrmks.mc.efscraft.common.crypt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class CryptTest {

    @Test
    public void testCrypt() throws IOException {

        NetworkSession.Server<OutputStream> sCtx = new NetworkSession.Server<>();
        NetworkSession.Client<OutputStream> cCtx = new NetworkSession.Client<>();

        byte[] data;
        PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream(outputStream);

        OutputStream dataOutput = null;

        Supplier<OutputStream> supplier = () -> new DataOutputStream(outputStream);
        {
            // server kick off
            // 1. generate a rsa keyPair, a random big number
            dataOutput = sCtx.handshakeHello(supplier);
        }

        {
            // client
            // 2. hash the server input, generate the DH pair and send the public key
            dataOutput = cCtx.handshakeHello(new DataInputStream(inputStream), supplier);
            Assertions.assertNotNull(dataOutput);
        }

        {
            // server
            // 3. validate the hash, generate dh pair, compute to aes cipher, and send the public key;
            dataOutput = sCtx.handshakeConfirm(new DataInputStream(inputStream), supplier);
            Assertions.assertNotNull(dataOutput);
        }

        {
            // client
            // 4. validate the dh public key, validate the sender, and compute to aes cipher;
            boolean flag = cCtx.handshakeConfirm(new DataInputStream(inputStream));
            Assertions.assertTrue(flag);
        }

        {
            // extra. A. verify the aes key;
//            byte[] server = sCtx.aesKey.getEncoded();
//            byte[] client = cCtx.aesKey.getEncoded();

//            Assertions.assertArrayEquals(server, client);
        }

        try {
            // extra. B. try the aes key;
            String msg = "IJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMNABCDEFGHIJKLMN";

            data = msg.getBytes(StandardCharsets.UTF_8);
            byte[] serverEnc = sCtx.encryptData(data);
            byte[] clientDec = cCtx.decryptData(serverEnc);
            Assertions.assertArrayEquals(data, clientDec);

            byte[] clientEnc = cCtx.encryptData(data);
            byte[] serverDec = sCtx.decryptData(clientEnc);

            Assertions.assertArrayEquals(data, serverDec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
