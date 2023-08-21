package com.github.mrmks.mc.efscraft.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        private NetworkPacket.ClientHandler<T, ?> cHandler;
        private NetworkPacket.ServerHandler<T, ?> sHandler;

        Node(Class<T> klass, Byte desc, Supplier<T> supplier, NetworkPacket.Codec<T> codec) {
            this.klass = klass;
            this.desc = desc;
            this.supplier = supplier;
            this.codec = codec;
        }

        void setHandler(NetworkPacket.ClientHandler<T, ?> handler) {
            if (this.cHandler == null && handler != null) this.cHandler = handler;
        }

        void setHandler(NetworkPacket.ServerHandler<T, ?> handler) {
            if (this.sHandler == null && handler != null) this.sHandler = handler;
        }

        NetworkPacket handle(InputStream input, MessageContext context) throws IOException {

            T packet = supplier.get();
            if (codec != null)
                codec.read(packet, input);

            NetworkPacket reply = null;
            if (context.isRemote()) {
                if (cHandler != null)
                    reply = cHandler.handlePacket(packet);
            } else {
                if (sHandler != null)
                    reply = sHandler.handlePacket(packet, context.getSender());
            }

            return reply;
        }

        void writeTo(T packet, OutputStream output) throws IOException {
            output.write(desc);
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
    public final NetworkPacket readInput(InputStream input, MessageContext context) throws IOException {
        byte desc = (byte) input.read();
        Node<?> node = descToNode.get(desc);
        if (node != null)
            return node.handle(input, context);

        return null;
    }

    public final <T extends NetworkPacket> boolean writeOutput(T packet, OutputStream output) throws IOException {
        Node<?> node = klassToNode.get(packet.getClass());
        if (node != null) {
            //noinspection unchecked
            ((Node<T>) node).writeTo(packet, output);
            return true;
        }
        return false;
    }
}
