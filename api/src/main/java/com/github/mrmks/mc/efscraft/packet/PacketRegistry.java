package com.github.mrmks.mc.efscraft.packet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@Deprecated
public class PacketRegistry {

    private final BiMap<Class<? extends IMessage>, Byte> clients = HashBiMap.create();
    private final BiMap<Class<? extends IMessage>, Byte> servers = HashBiMap.create();
    private byte serverIndex = 1, clientIndex = 1;

    protected final void register(Class<? extends IMessage> klass, boolean toRemote) {
        (toRemote ? servers : clients).putIfAbsent(klass, toRemote ? serverIndex++ : clientIndex++);
    }

    protected final void register(Class<? extends IMessage> klass) {
        register(klass, true);
        register(klass, false);
    }

    public final byte byClass(Class<? extends IMessage> klass, boolean toRemote) {

        if (klass == Hello.class) {
            return 0;   // Hello must be 0;
        }

        Byte ret = (toRemote ? servers : clients).get(klass);
        return ret == null ? -1 : ret;
    }

    public final IMessage byDesc(byte desc, boolean toRemote) {

        if (desc == 0) {
            return new Hello();     // and 0 must be Hello;
        }

        Class<? extends IMessage> klass =
                (toRemote ? servers : clients).inverse().getOrDefault(desc, null);

        if (klass == null) return null;
        try {
            return klass.newInstance();
        } catch (Throwable tr) {
            tr.printStackTrace();
        }

        return null;
    }

    public PacketRegistry() {
        register(SPacketPlayWith.class, true);
        register(SPacketStop.class, true);
        register(SPacketClear.class, true);
    }

    /**
     * The first packet to be sent, to validate the packet protocol version.
     * If this doesn't match, then any follow packets should be dropped off;
     */
    public static class Hello implements IMessage {

        private int protocol;
        public Hello() {}
        public Hello(int v) {
            this.protocol = v;
        }

        @Override
        public void read(DataInput stream) throws IOException {
            protocol = stream.readInt();
        }

        @Override
        public void write(DataOutput stream) throws IOException {
            stream.writeInt(protocol);
        }
    }
}
