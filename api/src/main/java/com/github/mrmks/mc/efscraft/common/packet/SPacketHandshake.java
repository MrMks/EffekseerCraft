package com.github.mrmks.mc.efscraft.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;

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
            public void read(Hello packet, InputStream stream) throws IOException {
            }

            @Override
            public void write(Hello packet, OutputStream stream) throws IOException {
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
            public void read(Confirm packet, InputStream stream) throws IOException {
            }

            @Override
            public void write(Confirm packet, OutputStream stream) throws IOException {
            }
        };

    }
}
