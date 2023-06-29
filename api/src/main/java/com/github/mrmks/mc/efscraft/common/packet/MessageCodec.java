package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessageCodec {

    private static class Node<T extends NetworkPacket> {
        private final Class<T> klass;
        private final Byte desc;
        private final Supplier<T> supplier;
        private final NetworkPacket.Codec<T> codec;
        @Deprecated private NetworkPacket.Handler<T, ?> handle;
        private NetworkPacket.ClientHandler<T, ?> cHandler;
        private NetworkPacket.ServerHandler<T, ?> sHandler;

        Node(Class<T> klass, Byte desc, Supplier<T> supplier, NetworkPacket.Codec<T> codec) {
            this.klass = klass;
            this.desc = desc;
            this.supplier = supplier;
            this.codec = codec;
        }

        @Deprecated
        void setHandler(NetworkPacket.Handler<T, ?> handle) {
            if (this.handle == null && handle != null) this.handle = handle;
        }

        void setHandler(NetworkPacket.ClientHandler<T, ?> handler) {
            if (this.cHandler == null && handler != null) this.cHandler = handler;
        }

        void setHandler(NetworkPacket.ServerHandler<T, ?> handler) {
            if (this.sHandler == null && handler != null) this.sHandler = handler;
        }

        NetworkPacket handle(DataInput input, MessageContext context) throws IOException {

            T packet = supplier.get();
            codec.read(packet, input);

            NetworkPacket reply = null;
            if (context.isRemote()) {
                if (cHandler != null)
                    reply = cHandler.handlePacket(packet);
                else if (handle != null)
                    reply = handle.handlePacket(packet, context);
            } else {
                if (sHandler != null)
                    reply = sHandler.handlePacket(packet, context.getSender());
                else if (handle != null)
                    reply = handle.handlePacket(packet, context);
            }

            return reply;
        }

        void writeTo(T packet, DataOutput output) throws IOException {
            output.writeByte(desc);
            if (codec != null)
                codec.write(packet, output);
        }
    }

    private final Map<Class<?>, Node<?>> klassToNode = new HashMap<>();
    private final Map<Byte, Node<?>> descToNode = new HashMap<>();
    private byte descIndex = 0;

    public MessageCodec() {

        register(PacketHello.class, PacketHello::new, PacketHello.CODEC);

        register(SPacketPlayWith.class, SPacketPlayWith::new, SPacketPlayWith.CODEC);
        register(SPacketPlayAt.class, SPacketPlayAt::new, SPacketPlayAt.CODEC);
        register(SPacketStop.class, SPacketStop::new, SPacketStop.CODEC);
        register(SPacketClear.class, SPacketClear::new, null);
        register(SPacketTrigger.class, SPacketTrigger::new, SPacketTrigger.CODEC);
    }

    private <T extends NetworkPacket> void register(Class<T> klass, Supplier<T> supplier, NetworkPacket.Codec<T> codec) {
        int modifier = klass.getModifiers();
        if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier))
            throw new IllegalArgumentException("We can't accept an abstract class or interface as message type!");

        Node<?> node = klassToNode.get(klass);
        if (node != null) throw new IllegalArgumentException();

        node = new Node<>(klass, descIndex++, supplier, codec);

        klassToNode.put(node.klass, node);
        descToNode.put(node.desc, node);
    }

    @Deprecated
    public final <T extends NetworkPacket> void register(Class<T> klass, NetworkPacket.Handler<T, ?> handle) {
        Node<?> node = klassToNode.get(klass);
        if (node != null) {
            //noinspection unchecked
            ((Node<T>) node).setHandler(handle);
        }
    }

    @Deprecated
    public final <T extends NetworkPacket> void register(Class<T> klass, BiConsumer<T, MessageContext> consumer) {
        register(klass, (packetIn, context) -> {consumer.accept(packetIn, context); return null;});
    }

    public final <T extends NetworkPacket> void registerClient(Class<T> klass, NetworkPacket.ClientHandler<T, ?> handle) {
        Node<?> node = klassToNode.get(klass);
        if (node != null)
            //noinspection unchecked
            ((Node<T>) node).setHandler(handle);
    }

    public final <T extends NetworkPacket> void registerClient(Class<T> klass, Consumer<T> consumer) {
        registerClient(klass, packetIn -> {consumer.accept(packetIn); return null;});
    }

    public final <T extends NetworkPacket> void registerServer(Class<T> klass, NetworkPacket.ServerHandler<T, ?> handle) {
        Node<?> node = klassToNode.get(klass);
        if (node != null)
            //noinspection unchecked
            ((Node<T>) node).setHandler(handle);
    }

    public final <T extends NetworkPacket> void registerServer(Class<T> klass, BiConsumer<T, UUID> consumer) {
        registerServer(klass, (packet, sender) -> {consumer.accept(packet, sender); return null;});
    }

    // in this case, responses always send back to the sender;
    public final <S extends DataInput> NetworkPacket readInput(S input, MessageContext context) throws IOException {
        byte desc = input.readByte();
        Node<?> node = descToNode.get(desc);
        if (node != null)
            return node.handle(input, context);

        return null;
    }

    public final <T extends NetworkPacket> boolean writeOutput(T packet, DataOutput output) throws IOException {
        Node<?> node = klassToNode.get(packet.getClass());
        if (node != null) {
            //noinspection unchecked
            ((Node<T>) node).writeTo(packet, output);
            return true;
        }
        return false;
    }
}
