package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class SPacketHandshake implements NetworkPacket {
    static class Hello extends SPacketHandshake {
        private PublicKey publicKey;
        private String serverId;
        private byte[] b;

        public Hello(PublicKey key, String serverId, byte[] b) {
            this.publicKey = key;
            this.serverId = serverId;
            this.b = b;
        }

        public Hello() {}

        static final Codec<Hello> CODEC = new Codec<Hello>() {
            @Override
            public void read(Hello packet, DataInput stream) throws IOException {
                int l; byte[] key;
                l = stream.readInt();
                key = new byte[l];
                stream.readFully(key);

                try {
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
                    KeyFactory factory = KeyFactory.getInstance(stream.readUTF());
                    packet.publicKey = factory.generatePublic(spec);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IOException(e);
                }

                packet.serverId = stream.readUTF();

                l = stream.readInt();
                stream.readFully(key = new byte[l]);
                packet.b = key;
            }

            @Override
            public void write(Hello packet, DataOutput stream) throws IOException {
                byte[] key = packet.publicKey.getEncoded();
                stream.writeInt(key.length);
                stream.write(key);
                stream.writeUTF(packet.publicKey.getAlgorithm());

                stream.writeUTF(packet.serverId);

                key = packet.b;
                stream.writeInt(key.length);
                stream.write(key);
            }
        };
    }

    static class Confirm extends SPacketHandshake {

        private PublicKey factor;
        private byte[] signedFactor;

        public Confirm() {}

        public Confirm(PublicKey factor, byte[] signedFactor) {
            this.factor = factor;
            this.signedFactor = signedFactor;
        }

        static final Codec<Confirm> CODEC = new Codec<Confirm>() {
            @Override
            public void read(Confirm packet, DataInput stream) throws IOException {
                int l; byte[] bytes;
                l = stream.readInt();
                stream.readFully(bytes = new byte[l]);

                try {
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                    KeyFactory factory = KeyFactory.getInstance(stream.readUTF());
                    packet.factor = factory.generatePublic(spec);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IOException(e);
                }

                l = stream.readInt();
                stream.readFully(bytes = new byte[l]);
                packet.signedFactor = bytes;
            }

            @Override
            public void write(Confirm packet, DataOutput stream) throws IOException {
                byte[] bytes = packet.factor.getEncoded();
                stream.writeInt(bytes.length);
                stream.write(bytes);
                stream.writeUTF(packet.factor.getAlgorithm());

                bytes = packet.signedFactor;
                stream.writeInt(bytes.length);
                stream.write(bytes);
            }
        };

    }
}
