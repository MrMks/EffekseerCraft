package com.github.mrmks.mc.efscraft.common.crypt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CryptTest {

    @Test
    public void testCrypt() throws IOException {

        NetworkSession.Server sCtx = new NetworkSession.Server();
        NetworkSession.Client cCtx = new NetworkSession.Client();

        byte[] data;
//        PipedOutputStream outputStream = new PipedOutputStream();
//        PipedInputStream inputStream = new PipedInputStream(outputStream);

//        OutputStream dataOutput = null;

        {
            // server kick off
            // 1. generate a rsa keyPair, a random big number
            data = sCtx.handshakeHello();
        }

        {
            // client
            // 2. hash the server input, generate the DH pair and send the public key
            data = cCtx.handshakeHello(data);
            Assertions.assertNotNull(data);
        }

        {
            // server
            // 3. validate the hash, generate dh pair, compute to aes cipher, and send the public key;
            data = sCtx.handshakeConfirm(data);
            Assertions.assertNotNull(data);
        }

        {
            // client
            // 4. validate the dh public key, validate the sender, and compute to aes cipher;
            data = cCtx.handshakeConfirm(data);
            Assertions.assertNotNull(data);
        }

        {
            // server
            // 5. confirm that the client has done the aes key generate;
            boolean flag = sCtx.handshakeDone(data);
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
