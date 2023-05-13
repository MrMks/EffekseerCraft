package com.github.mrmks.mc.efscraft.packet;

import com.github.mrmks.mc.efscraft.Constants;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class PacketHello implements IMessage {

    private int version;
    public PacketHello() {}

    @Override
    public void read(DataInput stream) throws IOException {
        version = stream.readInt();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeInt(Constants.PROTOCOL_VERSION);
    }

    public interface BooleanConsumer {
        void accept(boolean flag);
    }

    public static final class Handler implements IMessageHandler<PacketHello, PacketHello> {

        private final BooleanConsumer consumer;
        private final Set<UUID> clients;
        public Handler(BooleanConsumer validator, Set<UUID> clients) {
            this.consumer = validator;
            this.clients = clients;
        }

        public Handler(Set<UUID> clients) {
            this.consumer = it -> {throw new UnsupportedOperationException();};
            this.clients = clients;
        }

        @Override
        public PacketHello handlePacket(PacketHello packetIn, MessageContext context) {
            if (packetIn.version == Constants.PROTOCOL_VERSION) {
                if (context.isRemote()) {
                    consumer.accept(true);
                    return new PacketHello();
                } else {
                    clients.add(context.getSender());
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
