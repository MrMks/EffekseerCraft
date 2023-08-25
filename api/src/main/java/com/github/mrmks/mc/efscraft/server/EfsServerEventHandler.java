package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.server.event.EfsServerEvent;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

class EfsServerEventHandler<SV> {

    private static class Counter {
        int count;
        Counter(int init) {
            this.count = init;
        }

        boolean update() {
            return --count < 0;
        }
    }

    private final LogAdaptor logger;
    private final Map<UUID, HandshakeState> clients;
    private final Map<UUID, ?> sessions;
    private final Map<UUID, Counter> pending = new HashMap<>();

    private final EfsServer<SV, ?, ?, ?, ?, ?> server;

    EfsServerEventHandler(EfsServer<SV, ?, ?, ?, ?, ?> server) {
        this.server = server;
        this.logger = server.logger;
        this.clients = server.clients;
        this.sessions = server.sessions;
    }

    void receive(IEfsServerEvent event) {
        if (event instanceof EfsServerEvent) {

            if (event instanceof EfsServerEvent.Tick) {
                SV sv = (SV) ((EfsServerEvent.Tick<?>) event).getServer();
                tickAndUpdate(sv);
            } else if (event instanceof EfsServerEvent.Start) {
                server.commandHandler.updateFiles(((EfsServerEvent.Start<?>) event).getFiles());

                File keys = ((EfsServerEvent.Start<?>) event).getKeys();
                server.secretStore.reload(keys);
            } else if (event instanceof EfsServerEvent.Stop) {
                server.commandHandler.updateFiles(Collections.emptyList());
            }

        } else if (event instanceof EfsPlayerEvent) {

            UUID uuid = ((EfsPlayerEvent) event).getPlayer();
            if (event instanceof EfsPlayerEvent.Join)
                onLogin(uuid);
            else if (event instanceof EfsPlayerEvent.Leave)
                onLogout(uuid);
            else if (event instanceof EfsPlayerEvent.Verify) {
                onVerify(uuid);
            }

        }
    }

    protected final void onLogin(UUID uuid) {
        if (!clients.containsKey(uuid)) {
            pending.computeIfAbsent(uuid, it -> new Counter(10));
            clients.put(uuid, HandshakeState.START);
        }

        logger.logDebug("Player with uuid " + uuid + " begin to login");
    }

    protected final void onLogout(UUID uuid) {
        pending.remove(uuid);
        clients.remove(uuid);
        sessions.remove(uuid);
    }

    protected final void onVerify(UUID sender) {
        pending.remove(sender);
        logger.logInfo("Established connection to client with uuid: " + sender);
    }
    protected final void tickAndUpdate(SV sv) {
        ArrayList<UUID> list = null;
        Iterator<Map.Entry<UUID, Counter>> iterator = pending.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Counter> entry = iterator.next();

            if (entry.getValue().update()) {
                iterator.remove();

                UUID uuid = entry.getKey();
                HandshakeState state = clients.get(uuid);

                if (state == HandshakeState.START) {

                    if (sv != null)
                        server.packetHandler.sendToClient(sv, entry.getKey(), new PacketHello());

                    clients.put(uuid, HandshakeState.HELLO);

                    if (list == null) list = new ArrayList<>();

                    list.add(uuid);
                    logger.logInfo("Begin to connect to client with uuid " + uuid);
                } else if (state != HandshakeState.DONE) {
                    clients.remove(uuid);
                    sessions.remove(uuid);
                    logger.logInfo("Failed to establish the connection to client with uuid " + uuid + ": Timeout");
                }
            }
        }

        if (list != null)
            list.forEach(uuid -> pending.put(uuid, new Counter(600)));
    }

}
