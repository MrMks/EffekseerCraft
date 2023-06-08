package com.github.mrmks.mc.efscraft.common.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class MessageCodec {

    private static class Node<T extends NetworkPacket> {
        private final Class<T> klass;
        private final Byte desc;
        private final NetworkPacket.Codec<T> codec;
        private NetworkPacket.Handler<T, ?> handle;

        Node(Class<T> klass, Byte desc, NetworkPacket.Codec<T> codec) {
            this.klass = klass;
            this.desc = desc;
            this.codec = codec;
        }

        void setHandler(NetworkPacket.Handler<T, ?> handle) {
            if (this.handle == null && handle != null)
                this.handle = handle;
        }

        NetworkPacket handle(DataInput input, MessageContext context) throws IOException {

            if (handle == null) return null;

            T packet;
            try {
                packet = klass.newInstance();
                if (codec != null)
                    codec.read(packet, input);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }

            return handle.handlePacket(packet, context);
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

        register(PacketHello.class, PacketHello.Codec.INSTANCE);

        register(SPacketPlayWith.class, SPacketPlayWith.Codec.INSTANCE);
        register(SPacketPlayAt.class, SPacketPlayAt.Codec.INSTANCE);
        register(SPacketStop.class, SPacketStop.Codec.INSTANCE);
        register(SPacketClear.class, (NetworkPacket.Codec<SPacketClear>) null);
        register(SPacketTrigger.class, SPacketTrigger.Codec.INSTANCE);
    }

    private <T extends NetworkPacket> void register(Class<T> klass, NetworkPacket.Codec<T> codec) {
        int modifier = klass.getModifiers();
        if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier))
            throw new IllegalArgumentException("We can't accept an abstract class or interface as message type!");

        Node<?> node = klassToNode.get(klass);
        if (node != null) throw new IllegalArgumentException();

        node = new Node<>(klass, descIndex++, codec);

        klassToNode.put(node.klass, node);
        descToNode.put(node.desc, node);
    }

    public final <T extends NetworkPacket> void register(Class<T> klass, NetworkPacket.Handler<T, ?> handle) {
        Node<?> node = klassToNode.get(klass);
        if (node != null) {
            //noinspection unchecked
            ((Node<T>) node).setHandler(handle);
        }
    }

    // in this case, data always reply to the one who send inputs.
    public final <S extends DataInput> NetworkPacket writeInput(S input, MessageContext context) throws IOException {
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
