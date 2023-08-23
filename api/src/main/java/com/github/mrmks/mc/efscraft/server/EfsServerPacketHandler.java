package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.crypt.NetworkSession;
import com.github.mrmks.mc.efscraft.common.packet.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class EfsServerPacketHandler<SV, EN, PL extends EN, DO extends OutputStream> {

    private final EfsServer<SV, ?, EN, PL, ?, DO> server;
    private final MessageCodec codec;
    private final boolean autoReply;
    private final Map<UUID, NetworkSession.Server> sessions;
    private final Map<UUID, HandshakeState> states;

    EfsServerPacketHandler(EfsServer<SV, ?, EN, PL, ?, DO> server, boolean autoReply) {
        this.server = server;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;

        this.sessions = new ConcurrentHashMap<>();
        this.states = server.clients;

        init();
    }

    void init() {
        codec.registerServer(PacketHello.class, this::dispatchHandshake);
        codec.registerServer(PacketHandshake.CHello.class, this::dispatchHandshake);
        codec.registerServer(PacketHandshake.CConfirmAndRequest.class, this::dispatchHandshake);
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

    private NetworkPacket dispatchHandshake(NetworkPacket packet, UUID uuid) {
        HandshakeState state = states.get(uuid);

        if (state == null) return null;

        if (state == HandshakeState.START || state == HandshakeState.DONE)
            states.put(uuid, state = HandshakeState.ERROR);

        if (state == HandshakeState.VERIFY && packet instanceof PacketHello) {
            if (((PacketHello) packet).version == Constants.PROTOCOL_VERSION) {
                states.put(uuid, HandshakeState.HELLO);
                NetworkSession.Server session = new NetworkSession.Server();
                sessions.put(uuid, session);
                return new PacketHandshake.SHello(session.handshakeHello());
            }
        }
        else if (state == HandshakeState.HELLO && packet instanceof PacketHandshake.CHello) {
            NetworkSession.Server session = sessions.get(uuid);
            if (session != null) {
                byte[] data = ((PacketHandshake.CHello) packet).getData();
                try {
                    data = session.handshakeConfirm(data);
                    states.put(uuid, HandshakeState.CONFIRM);
                    return new PacketHandshake.SConfirm(data);
                } catch (IOException e) {}
            }
        }
        else if (state == HandshakeState.CONFIRM && packet instanceof PacketHandshake.CConfirmAndRequest) {
            NetworkSession.Server session = sessions.get(uuid);
            if (session != null) {
                // todo: validate client, read request and return response;
                byte[] data = ((PacketHandshake.CConfirmAndRequest) packet).getData();
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
                try {
                    data = new byte[input.readInt()];
                    int len = input.read(data);
                    if (len != data.length) throw new EOFException();

                    boolean flag = session.handshakeDone(data);
                    if (flag) {
                        states.put(uuid, HandshakeState.DONE); // skip complete

                        int size = input.readInt();
                        String[] digests = new String[size];
                        for (int i = 0; i < size; i++)
                            digests[i] = input.readUTF();

                        Map<String, byte[]> ret = new HashMap<>();
                        for (String d : digests) {
                            byte[] k = server.decryptKeys.get(d);
                            if (k != null)
                                ret.put(d, k);
                        }

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        DataOutputStream stream = new DataOutputStream(outputStream);

                        stream.writeInt(ret.size());
                        for (Map.Entry<String, byte[]> entry : ret.entrySet()) {
                            stream.writeUTF(entry.getKey());
                            stream.writeInt(entry.getValue().length);
                            stream.write(entry.getValue());
                        }

                        return new PacketHandshake.SResponse(session.encryptData(outputStream.toByteArray()));
                    }
                } catch (IOException e) {}
            }
        }

        sessions.remove(uuid);
        states.remove(uuid);
        return PacketHandshakeDisconnect.INSTANCE;
    }

    private void handleDisconnect(PacketHandshakeDisconnect packet, UUID uuid) {
        sessions.remove(uuid);
        states.remove(uuid);
    }

}
