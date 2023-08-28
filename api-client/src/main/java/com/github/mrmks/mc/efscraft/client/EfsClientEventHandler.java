package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.client.event.EfsDisconnectEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;
import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.crypt.NetworkSession;
import com.github.mrmks.mc.efscraft.common.packet.PacketDecrypt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

class EfsClientEventHandler {

    private final EfsClient<?, ?, ?> client;

    EfsClientEventHandler(EfsClient<?,?,?> client) {
        this.client = client;
    }

    void receive(IEfsClientEvent event) {
        if (event instanceof EfsRenderEvent) {
            EfsRenderEvent rEvent = (EfsRenderEvent) event;

            if (rEvent.getPhase() == EfsRenderEvent.Phase.START) {
                if (!rEvent.isGamePause())
                    client.renderer.update(rEvent.getPartial(), rEvent.getNanoNow(), rEvent.isGamePause(), rEvent.getMatProj(), rEvent.getMatModel());
            }

            client.adaptor.drawEffect(rEvent, client.renderer.getProgram());

        } else if (event instanceof EfsResourceEvent) {

            if (event instanceof EfsResourceEvent.Reload) {
                client.resources.onReload();

                NetworkSession.Client session = client.session;

                if (client.handshakeState == HandshakeState.DONE && session != null) {

                    Set<String> digests = client.resources.encryptedDigests();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    DataOutputStream stream = new DataOutputStream(outputStream);

                    try {
                        stream.writeInt(digests.size());
                        for (String d : digests) stream.writeUTF(d);

                        byte[] data = outputStream.toByteArray();
                        data = session.encryptData(data);

                        client.packetHandler.sendToServer(new PacketDecrypt.CRequest(data));
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }

        } else if (event instanceof EfsDisconnectEvent) {
            client.handshakeState = HandshakeState.START;
            client.session = null;
            client.renderer.stopAll();

            client.resources.onReload();
        }
    }



}
