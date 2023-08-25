package com.github.mrmks.mc.efscraft.common.packet;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PacketDecrypt implements NetworkPacket {

    private byte[] data;

    PacketDecrypt() {}
    PacketDecrypt(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public static class SResponse extends PacketDecrypt {
        SResponse() {}
        public SResponse(byte[] data) { super(data); }
    }

    public static class CRequest extends PacketDecrypt {
        CRequest() {}
        public CRequest(byte[] data) { super(data); }
    }

    static final Codec<PacketDecrypt> CODEC = new Codec<PacketDecrypt>() {
        @Override
        public void read(PacketDecrypt packet, InputStream stream) throws IOException {
            packet.data = ByteStreams.toByteArray(stream);
        }

        @Override
        public void write(PacketDecrypt packet, OutputStream stream) throws IOException {
            stream.write(packet.data);
        }
    };
}
