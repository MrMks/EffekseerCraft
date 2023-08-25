package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.crypt.NetworkSession;
import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.math.Matrix4f;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class EfsClientPacketHandler<DO extends OutputStream> {

    private final MessageCodec codec;
    private final boolean autoReply;
    private final EfsClient<?, ?, DO> client;
    private final EfsDrawingQueue<?> queue;

    EfsClientPacketHandler(EfsClient<?, ?, DO> client, boolean autoReply, EfsDrawingQueue<?> queue) {
        this.client = client;
        this.codec = new MessageCodec();
        this.autoReply = autoReply;
        this.queue = queue;

        codec.registerClient(PacketHello.class, handshakeHandler(this::handleHandshakeHello, HandshakeState.START, HandshakeState.BEGIN));
        codec.registerClient(PacketHandshake.SHello.class, handshakeHandler(this::handleHandshakeStart, HandshakeState.BEGIN, HandshakeState.CONFIRM));
        codec.registerClient(PacketHandshake.SConfirm.class, handshakeHandler(this::handleHandshakeConfirm, HandshakeState.CONFIRM, HandshakeState.COMPLETE));
        codec.registerClient(PacketHandshake.SResponse.class, handshakeHandler(this::handleHandshakeComplete, HandshakeState.COMPLETE, HandshakeState.DONE));
        codec.registerClient(PacketHandshakeDisconnect.class, this::handleDisconnect);

        codec.registerClient(SPacketPlayWith.class, this::handlePlayWith);
        codec.registerClient(SPacketPlayAt.class, this::handlePlayAt);
        codec.registerClient(SPacketTrigger.class, this::handleTrigger);
        codec.registerClient(SPacketStop.class, this::handleStop);
        codec.registerClient(SPacketClear.class, this::handleClear);
    }

    DO receive(InputStream dataInput) throws IOException {
        NetworkPacket packet = codec.readInput(dataInput);

        if (autoReply && packet != null) {
            sendToServer(packet);
            return null;
        } else {
            return writePacketOutput(packet);
        }
    }

    void sendToServer(NetworkPacket packet) {
        try {
            DO output = writePacketOutput(packet);
            if (output != null) {
                client.adaptor.sendPacket(output);
                client.adaptor.closeOutput(output);
            }
        } catch (IOException e) {
            client.logger.logWarning("Unable to encode a packet to stream", e);
        }
    }

    private DO writePacketOutput(NetworkPacket packet) throws IOException {
        if (packet != null) {
            DO output = client.adaptor.createOutput();
            if (codec.writeOutput(packet, output)) {
                return output;
            }

            client.adaptor.closeOutput(output);
        }

        return null;
    }

    private interface Func<T extends NetworkPacket> {
        NetworkPacket accept(T packet, Runnable trigger);
    }

    private <T extends NetworkPacket> NetworkPacket.ClientHandler<T, NetworkPacket> handshakeHandler(Func<T> func, HandshakeState fState, HandshakeState tState) {
        return packet -> {
            HandshakeState curState = client.handshakeState;
            if (curState == null || curState.ordinal() > fState.ordinal())
                return null;

            if (curState != fState) {
                client.session = null;
                client.handshakeState = null;

                return PacketHandshakeDisconnect.INSTANCE;
            }

            boolean[] flags = { false };
            NetworkPacket ret = null;
            try {
                ret = func.accept(packet, () -> flags[0] = true);
            } catch (Throwable tr) {
                client.logger.logWarning("Unable to handle a handshake packet", tr);
                flags[0] = false;
            }

            if (flags[0]) {

                client.logger.logDebug(String.format("Handshake(Client): %s -> %s", fState, tState));

                client.handshakeState = tState;
                return ret;
            } else {

                client.logger.logDebug(String.format("Handshake(Client) Failed: %s -> %s", fState, "ERROR"));

                client.session = null;
                client.handshakeState = null;

                return PacketHandshakeDisconnect.INSTANCE;
            }

        };
    }

    private NetworkPacket handleHandshakeHello(PacketHello packet, Runnable t) {
        if (packet.version != Constants.PROTOCOL_VERSION)
            return null;

        client.session = new NetworkSession.Client();
        t.run();
        return new PacketHello();
    }

    private NetworkPacket handleHandshakeStart(PacketHandshake.SHello packet, Runnable t) {
        byte[] data = packet.getData();
        NetworkSession.Client session = client.session;

        if (data == null || session == null)
            return null;

        try {
            data = session.handshakeHello(data);
        } catch (IOException e) {
            data = null;
        }

        if (data == null)
            return null;

        t.run();
        return new PacketHandshake.CHello(data);
    }

    private NetworkPacket handleHandshakeConfirm(PacketHandshake.SConfirm packet, Runnable t) {
        byte[] data = packet.getData();
        NetworkSession.Client session = client.session;

        if (data == null || session == null)
            return null;

        try {
            data = session.handshakeConfirm(data);
        } catch (IOException e) {
            data = null;
        }

        if (data == null)
            return null;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(outputStream);

        try {
            stream.writeInt(data.length);
            stream.write(data);

            Set<String> set = client.resources.encryptedDigests();
            stream.writeInt(set.size());
            for (String d : set) stream.writeUTF(d);

        } catch (IOException e) {
            data = null;
        }

        if (data == null)
            return null;

        t.run();
        data = session.encryptData(outputStream.toByteArray());
        return new PacketHandshake.CConfirmAndRequest(data);
    }

    private NetworkPacket handleHandshakeComplete(PacketHandshake.SResponse packet, Runnable t) {
        byte[] data = packet.getData();
        NetworkSession.Client session = client.session;

        if (data == null || session == null)
            return null;

        Map<String, byte[]> map = new HashMap<>();
        try(DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            int len = input.readInt();

            for (int i = 0; i < len; i ++) {
                String d = input.readUTF();
                byte[] v = new byte[input.readInt()];
                if (v.length != input.read(v)) throw new EOFException();

                v = session.decryptData(v);

                map.put(d, v);
            }

        } catch (IOException e) {
            map = null;
        }

        if (map == null)
            return null;

        t.run();
        client.resources.receiveDecryptKey(map);
        return null;
    }

    private void handleDisconnect(PacketHandshakeDisconnect packet) {
        client.handshakeState = HandshakeState.ERROR;
        client.session = null;
    }

    private void handlePlayWith(SPacketPlayWith packet) { client.adaptor.schedule(() -> handlePlayWith0(packet)); }
    private void handlePlayWith0(SPacketPlayWith packet) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(packet.getLocalPosition())
                .rotateMC(packet.getLocalRotation())
                .scale(packet.getScale());


        queue.commandPlayWith(packet.getKey(), packet.getEmitter(), packet.getEffect(), packet.conflictOverwrite(),
                packet.getFrameSkip(), packet.getLifespan(), packet.getDynamics(),
                base, packet.getModelPosition(), packet.getModelRotation(),
                packet.getTarget(),
                packet.followX(), packet.followY(), packet.followZ(), packet.followYaw(), packet.followPitch(),
                packet.isInheritYaw(), packet.isInheritPitch(),
                packet.isUseHead(), packet.isUseRender());
    }

    private void handlePlayAt(SPacketPlayAt packet) { client.adaptor.schedule(() -> handlePlayAt0(packet)); }
    private void handlePlayAt0(SPacketPlayAt packet) {

        Matrix4f base = new Matrix4f().identity()
                .translatef(packet.getLocalPosition())
                .rotateMC(packet.getLocalRotation())
                .scale(packet.getScale());

        queue.commandPlayAt(
                packet.getKey(), packet.getEmitter(), packet.getEffect(), packet.conflictOverwrite(),
                packet.getFrameSkip(), packet.getLifespan(), packet.getDynamics(),
                base, packet.getModelPosition(), packet.getModelRotation(),
                packet.getTargetPos(), packet.getTargetRot()
        );
    }

    private void handleTrigger(SPacketTrigger packet) { client.adaptor.schedule(() -> handleTrigger0(packet)); }
    private void handleTrigger0(SPacketTrigger packet) {
        queue.commandTrigger(packet.getKey(), packet.getEmitter(), packet.getTrigger());
    }

    private void handleStop(SPacketStop packet) { client.adaptor.schedule(() -> handleStop0(packet)); }
    private void handleStop0(SPacketStop packet) {
        queue.commandStop(packet.getKey(), packet.getEmitter());
    }

    private void handleClear(SPacketClear packet) { client.adaptor.schedule(() -> handleClear0(packet)); }
    private void handleClear0(SPacketClear packet) {
        queue.commandClear();
    }
}
