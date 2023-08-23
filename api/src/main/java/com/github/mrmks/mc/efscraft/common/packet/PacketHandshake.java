package com.github.mrmks.mc.efscraft.common.packet;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class PacketHandshake implements NetworkPacket {

    private byte[] data;

    public PacketHandshake() {}

    public PacketHandshake(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public static class SHello extends PacketHandshake {
        public SHello() {}
        public SHello(byte[] data) { super(data); }
    }

    public static class CHello extends PacketHandshake {
        public CHello() {}
        public CHello(byte[] data) {super(data);}
    }

    public static class SConfirm extends PacketHandshake {
        public SConfirm() {}
        public SConfirm(byte[] data) { super(data); }
    }

    public static class CConfirmAndRequest extends PacketHandshake {
        public CConfirmAndRequest() {}
        public CConfirmAndRequest(byte[] data) { super(data); }
    }

    public static class SResponse extends PacketHandshake {
        public SResponse() {}
        public SResponse(byte[] data) { super(data); }
    }

    static final Codec<PacketHandshake> CODEC = new Codec<PacketHandshake>() {
        @Override
        public void read(PacketHandshake packet, InputStream stream) throws IOException {
            packet.data = ByteStreams.toByteArray(stream);
        }

        @Override
        public void write(PacketHandshake packet, OutputStream stream) throws IOException {
            stream.write(packet.data);
        }
    };

}
