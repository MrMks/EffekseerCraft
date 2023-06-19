package com.github.mrmks.mc.efscraft.packet;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.ILogAdaptor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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

    public enum State {
        WAITING_FOR_REPLY, COMPLETE
    }

    public static final class Handler implements IMessageHandler<PacketHello, PacketHello> {

        private final BooleanConsumer consumer;
        private final Map<UUID, State> clients;
        private ILogAdaptor logger;
        public Handler(BooleanConsumer validator, Map<UUID, State> clients, ILogAdaptor logger) {
            this.consumer = validator;
            this.clients = clients;
            this.logger = logger;
        }

        public Handler(Map<UUID, State> clients, ILogAdaptor logger) {
            this.consumer = it -> {throw new UnsupportedOperationException();};
            this.clients = clients;
            this.logger = logger;
        }

        @Override
        public PacketHello handlePacket(PacketHello packetIn, MessageContext context) {
            if (packetIn.version == Constants.PROTOCOL_VERSION) {
                if (context.isRemote()) {
                    consumer.accept(true);
                    return new PacketHello();
                } else {
                    if (clients.get(context.getSender()) == State.WAITING_FOR_REPLY) {
                        clients.put(context.getSender(), State.COMPLETE);
                        logger.logDebug("Established connection to client with uuid " + context.getSender());
                    } else {
                        logger.logDebug("Received hello packet from unexpected client " + context.getSender());
                    }
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
