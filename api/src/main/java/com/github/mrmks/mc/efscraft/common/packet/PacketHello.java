package com.github.mrmks.mc.efscraft.common.packet;

import com.github.mrmks.mc.efscraft.server.IEfsServerEvent;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;

import java.io.*;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketHello implements NetworkPacket {

    private int version;
    public PacketHello() {}

    static final NetworkPacket.Codec<PacketHello> CODEC = new NetworkPacket.Codec<PacketHello>() {
        @Override
        public void read(PacketHello packet, InputStream stream) throws IOException {
            packet.version = new DataInputStream(stream).readInt();
        }

        @Override
        public void write(PacketHello packet, OutputStream stream) throws IOException {
            new DataOutputStream(stream).writeInt(Constants.PROTOCOL_VERSION);
        }
    };

    public interface BooleanConsumer {
        void accept(boolean flag);
    }

    public enum State {
        WAITING_FOR_REPLY, COMPLETE
    }

    public static final class ClientHandler implements NetworkPacket.ClientHandler<PacketHello, PacketHello> {

        private final BooleanConsumer setter;
        public ClientHandler(BooleanConsumer setter) {
            this.setter = setter;
        }

        @Override
        public PacketHello handlePacket(PacketHello packetIn) {
            boolean flag = packetIn.version == Constants.PROTOCOL_VERSION;
            setter.accept(flag);

            return flag ? new PacketHello() : null;
        }
    }

    public static final class ServerHandler implements NetworkPacket.ServerHandler<PacketHello, NetworkPacket> {

        private final Consumer<IEfsServerEvent> consumer;

        public ServerHandler(Consumer<IEfsServerEvent> consumer) {
            this.consumer = consumer;
        }

        @Override
        public NetworkPacket handlePacket(PacketHello packet, UUID sender) {
            IEfsServerEvent event = new EfsPlayerEvent.Verify(sender, packet.version);
            consumer.accept(event);

            return null;
        }
    }
}
