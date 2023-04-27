package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class MessageCodec {

    private static class Node<T extends IMessage> {
        private final Class<T> klass;
        private final Byte desc;
        private IMessageHandler<T, ?> handle;

        Node(Class<T> klass, Byte desc) {
            this.klass = klass;
            this.desc = desc;
        }

        void setHandler(IMessageHandler<T, ?> handle) {
            if (this.handle == null && handle != null)
                this.handle = handle;
        }

        IMessage handle(DataInput input, MessageContext context) throws IOException {

            if (handle == null) return null;

            T packet;
            try {
                packet = klass.newInstance();
                packet.read(input);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }

            return handle.handlePacket(packet, context);
        }
    }

    private final Map<Class<?>, Node<?>> klassToNode = new HashMap<>();
    private final Map<Byte, Node<?>> descToNode = new HashMap<>();
    private byte descIndex = 0;

    public MessageCodec() {

        register(PacketHello.class);

        register(SPacketPlayWith.class);
        register(SPacketPlayAt.class);
        register(SPacketStop.class);
        register(SPacketClear.class);
    }

    private void register(Class<? extends IMessage> klass) {
        int modifier = klass.getModifiers();
        if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier))
            throw new IllegalArgumentException("We can't accept an abstract class or interface as message type!");

        Node<?> node = klassToNode.get(klass);
        if (node != null) throw new IllegalArgumentException();

        node = new Node<>(klass, descIndex++);

        klassToNode.put(node.klass, node);
        descToNode.put(node.desc, node);
    }

    public final <T extends IMessage> void register(Class<T> klass, IMessageHandler<T, ?> handle) {
        Node<?> node = klassToNode.get(klass);
        if (node != null) {
            //noinspection unchecked
            ((Node<T>) node).setHandler(handle);
        }
    }

    // in this case, data always reply to the one who send inputs.
    public final <S extends DataInput> IMessage writeInput(S input, MessageContext context) throws IOException {
        byte desc = input.readByte();
        Node<?> node = descToNode.get(desc);
        if (node != null)
            return node.handle(input, context);

        return null;
    }

    public final boolean writeOutput(IMessage packet, DataOutput output) throws IOException {
        Node<?> node = klassToNode.get(packet.getClass());
        if (node != null) {
            output.writeByte(node.desc);
            packet.write(output);
            return true;
        }
        return false;
    }
}
