package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.crypt.NetworkSession;
import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class EfsServerPacketHandler<SV, EN, PL extends EN, DO extends OutputStream> {

    private final EfsServer<SV, ?, EN, PL, ?, DO> server;
    private final MessageCodec codec;
    private final boolean autoReply;
    private final Map<UUID, HandshakeState> states;
    private final Map<UUID, NetworkSession.Server> sessions;

    EfsServerPacketHandler(EfsServer<SV, ?, EN, PL, ?, DO> server, boolean autoReply) {
        this.server = server;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;

        this.states = server.clients;
        this.sessions = server.sessions;

        init();
    }

    void init() {
        codec.registerServer(PacketHello.class, handshakeHandler(this::handleHandshakeHello, HandshakeState.HELLO, HandshakeState.BEGIN));
        codec.registerServer(PacketHandshake.CHello.class, handshakeHandler(this::handleHandshakeVerify, HandshakeState.BEGIN, HandshakeState.CONFIRM));
        codec.registerServer(PacketHandshake.CConfirmAndRequest.class, handshakeHandler(this::handleHandshakeConfirm, HandshakeState.CONFIRM, HandshakeState.DONE));
        codec.registerServer(PacketHandshakeDisconnect.class, this::handleDisconnect);
    }

    DO receive(SV sv, PL receiver, InputStream dataIn) throws IOException {
        UUID uuid = server.adaptor.getEntityUUID(receiver);
        NetworkPacket packet = codec.readInput(dataIn, uuid);

        if (autoReply && packet != null) {
            sendToClient(sv, receiver, packet);
            return null;
        } else {
            return writePacketOutput(packet);
        }
    }

    void sendToClient(SV sv, UUID receiver, NetworkPacket packet) {
        PL pl = server.adaptor.getPlayer(sv, receiver);
        if (pl == null)
            return;

        sendToClient(sv, pl, packet);
    }

    void sendToClient(SV svr, PL player, NetworkPacket packet) {
        sendToClient(svr, Collections.singleton(player), any -> true, packet);
    }

    void sendToClient(SV svr, Collection<PL> players, Predicate<PL> test, NetworkPacket packet) {
        try {
            DO output = writePacketOutput(packet);
            if (output != null) {
                server.adaptor.sendPacket(svr, players, test, output);
                server.adaptor.closeOutput(output);
            }
        } catch (IOException e) {
            server.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    private DO writePacketOutput(NetworkPacket packet) throws IOException {
        if (packet != null) {
            DO output = server.adaptor.createOutput();
            if (codec.writeOutput(packet, output)) {
                return output;
            }
            server.adaptor.closeOutput(output);
        }

        return null;
    }

    private void handleDisconnect(PacketHandshakeDisconnect packet, UUID uuid) {
        sessions.remove(uuid);
        states.remove(uuid);
    }

    private interface Func<T extends NetworkPacket> {
        NetworkPacket accept(T packet, UUID uuid, Runnable trigger);
    }

    private <T extends NetworkPacket> NetworkPacket.ServerHandler<T, NetworkPacket> handshakeHandler(Func<T> func, HandshakeState stateFrom, HandshakeState stateTo) {
        return (packet, sender) -> {
            HandshakeState curState = states.get(sender);
            if (curState == null || curState.ordinal() > stateFrom.ordinal())
                return null;

            if (curState != stateFrom) {
                states.remove(sender);
                sessions.remove(sender);

                return PacketHandshakeDisconnect.INSTANCE;
            }

            boolean[] flags = { false };
            NetworkPacket ret = func.accept(packet, sender, () -> flags[0] = true);

            if (flags[0]) {
                states.put(sender, stateTo);

                if (stateTo == HandshakeState.DONE) {
                    server.receiveEvent(new EfsPlayerEvent.Verify(sender));
                }

                return ret;
            } else {
                states.remove(sender);
                sessions.remove(sender);

                return PacketHandshakeDisconnect.INSTANCE;
            }

        };
    }

    private NetworkPacket handleHandshakeHello(PacketHello packet, UUID sender, Runnable trigger) {
        if (packet.version == Constants.PROTOCOL_VERSION) {
            trigger.run();

            NetworkSession.Server session;
            sessions.put(sender, session = new NetworkSession.Server());
            return new PacketHandshake.SHello(session.handshakeHello());
        }

        return null;
    }

    private NetworkPacket handleHandshakeVerify(PacketHandshake.CHello packet, UUID sender, Runnable trigger) {
        byte[] data = packet.getData();
        NetworkSession.Server session = sessions.get(sender);

        if (session == null || data == null) {
            return null;
        }

        try {
            data = session.handshakeConfirm(data);
        } catch (IOException e) {
            data = null;
        }

        if (data != null) {
            trigger.run();
            return new PacketHandshake.SConfirm(data);
        }

        return null;
    }

    private NetworkPacket handleHandshakeConfirm(PacketHandshake.CConfirmAndRequest packet, UUID sender, Runnable r) {
        byte[] data = packet.getData();
        NetworkSession.Server session = sessions.get(sender);

        if (session == null || data == null) {
            return null;
        }

        data = session.decryptData(data);

        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
            int len = input.readInt();
            byte[] verify = new byte[len];
            len = input.read(verify);

            if (len != verify.length) throw new EOFException();

            boolean flag = session.handshakeDone(verify);

            if (!flag) return null;

            len = input.readInt();
            String[] digests = new String[len];

            for (int i = 0; i < len; i++)
                digests[i] = input.readUTF();

            Map<String, byte[]> map = new HashMap<>();
            for (String d : digests) {
                byte[] key = server.decryptKeys.get(d);
                if (key != null) map.put(d, key);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(outputStream);

            for (Map.Entry<String, byte[]> entry : map.entrySet()) {
                stream.writeUTF(entry.getKey());
                stream.writeInt(entry.getValue().length);
                stream.write(entry.getValue());
            }

            data = outputStream.toByteArray();
            data = session.encryptData(data);

            r.run();

            return new PacketHandshake.SResponse(data);
        } catch (IOException e) {}

        return null;
    }
}
