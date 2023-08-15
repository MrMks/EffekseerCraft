package com.github.mrmks.mc.efscraft.common.packet;

import com.github.mrmks.mc.efscraft.server.IEfsServerEvent;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketHello implements NetworkPacket {

    private int version;
    public PacketHello() {}

    static final NetworkPacket.Codec<PacketHello> CODEC = new NetworkPacket.Codec<PacketHello>() {
        @Override
        public void read(PacketHello packet, DataInput stream) throws IOException {
            packet.version = stream.readInt();
        }

        @Override
        public void write(PacketHello packet, DataOutput stream) throws IOException {
            stream.writeInt(Constants.PROTOCOL_VERSION);
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

        private final Map<UUID, State> clients;
        private final LogAdaptor logger;

        public ServerHandler(Map<UUID, State> clients, LogAdaptor logger) {
            this.clients = clients;
            this.logger = logger;
        }

        @Override
        public NetworkPacket handlePacket(PacketHello packet, UUID sender) {

            if (packet.version == Constants.PROTOCOL_VERSION) {
                if (clients.get(sender) == State.WAITING_FOR_REPLY) {
                    clients.put(sender, State.COMPLETE);
                    logger.logInfo("Established connection to client with uuid " + sender);
                } else {
                    logger.logWarning("Received hello packet from unexpected client " + sender);
                }
            }

            return null;
        }
    }

    public static final class InternalServerHandler implements NetworkPacket.ServerHandler<PacketHello, NetworkPacket> {

        private final Consumer<IEfsServerEvent> consumer;

        public InternalServerHandler(Consumer<IEfsServerEvent> consumer) {
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
