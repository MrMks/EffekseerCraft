package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.crypt.NetworkSession;
import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.math.Matrix4f;

import java.io.*;
import java.util.HashMap;
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

        codec.registerClient(PacketHello.class, this::dispatchHandshake);
        codec.registerClient(PacketHandshake.SHello.class, this::dispatchHandshake);
        codec.registerClient(PacketHandshake.SConfirm.class, this::dispatchHandshake);
        codec.registerClient(PacketHandshake.SResponse.class, this::dispatchHandshake);
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

    private void dispatchHandshake(NetworkPacket packet) {
        client.adaptor.schedule(() -> dispatchHandshake0(packet));
    }

    private void dispatchHandshake0(NetworkPacket packet) {
        HandshakeState state = client.handshakeState;
        if (state == null || state == HandshakeState.ERROR)
            return;

        if (state == HandshakeState.START) {
            if (packet instanceof PacketHello) {
                if (((PacketHello) packet).version == Constants.PROTOCOL_VERSION) {
                    // skip verify state;
                    client.handshakeState = HandshakeState.HELLO;
                    client.session = new NetworkSession.Client();

                    client.packetHandler.sendToServer(new PacketHello());
                    return;
                }
            }

        } else if (packet instanceof PacketHandshake && client.session != null) {
            byte[] data = ((PacketHandshake) packet).getData();

            try {
                if (state == HandshakeState.HELLO && packet instanceof PacketHandshake.SHello) {
                    NetworkPacket reply = new PacketHandshake.CHello(client.session.handshakeHello(data));
                    sendToServer(reply);
                    client.handshakeState = HandshakeState.CONFIRM;
                    return;
                } else if (state == HandshakeState.CONFIRM && packet instanceof PacketHandshake.SConfirm) {
                    byte[] bytes = client.session.handshakeConfirm(data);
                    if (bytes != null) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        DataOutputStream stream = new DataOutputStream(output);

                        stream.writeInt(bytes.length);
                        stream.write(bytes);

                        Set<String> set = client.resources.encryptedDigests();
                        stream.writeInt(set.size());
                        for (String d : set)
                            stream.writeUTF(d);

                        NetworkPacket reply = new PacketHandshake.CConfirmAndRequest(output.toByteArray());
                        sendToServer(reply);
                        client.handshakeState = HandshakeState.COMPLETE;
                        return;
                    }
                } else if (state == HandshakeState.COMPLETE && packet instanceof PacketHandshake.SResponse) {
                    data = client.session.decryptData(data);

                    DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
                    HashMap<String, byte[]> map = new HashMap<>();
                    try {
                        int size = stream.readInt();

                        for (int i = 0; i < size; i++) {
                            String k = stream.readUTF();
                            int len = stream.readInt();
                            byte[] v = new byte[len];

                            len = stream.read(v);

                            if (len != v.length)
                                throw new EOFException();

                            map.put(k, v);
                        }
                        client.resources.receiveDecryptKey(map);
                        client.handshakeState = HandshakeState.DONE;
                        return;
                    } catch (IOException e) {
                        // error happened;
                    }
                }
            } catch (IOException e) {
                // goto error;
            }
        }

        sendToServer(PacketHandshakeDisconnect.INSTANCE);
        client.handshakeState = HandshakeState.ERROR;
        client.session = null;
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
