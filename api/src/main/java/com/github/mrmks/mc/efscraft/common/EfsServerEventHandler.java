package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.common.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.common.event.EfsTickEvent;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;

import java.util.*;

public class EfsServerEventHandler {

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
    private final Map<UUID, PacketHello.State> clients;
    private final Map<UUID, Counter> pending = new HashMap<>();

    private final EfsServer<?, ?, ?, ?, ?, ?, ?> server;

    EfsServerEventHandler(EfsServer<?, ?, ?, ?, ?, ?, ?> server) {
        this.server = server;
        this.logger = server.logger;
        this.clients = server.clients;
    }

    protected EfsServerEventHandler(Map<UUID, PacketHello.State> clients, LogAdaptor logger) {
        this.clients = clients;
        this.logger = logger;
        this.server = null;
    }

    void receive(IEfsServerEvent event) {
        if (event instanceof EfsTickEvent) {
            tickAndUpdate();
        } else if (event instanceof EfsPlayerEvent) {
            UUID uuid = ((EfsPlayerEvent) event).getPlayer();
            if (event instanceof EfsPlayerEvent.Join)
                onLogin(uuid);
            else if (event instanceof EfsPlayerEvent.Leave)
                onLogout(uuid);
            else if (event instanceof EfsPlayerEvent.Verify) {
                int ver = ((EfsPlayerEvent.Verify) event).getVersion();

                onVerify(uuid, ver);
            }
        }
    }

    protected final void onLogin(UUID uuid) {
        if (!clients.containsKey(uuid))
            pending.computeIfAbsent(uuid, it -> new Counter(10));

        logger.logDebug("Player with uuid " + uuid + " begin to login");
    }

    protected final void onLogout(UUID uuid) {
        pending.remove(uuid);
        clients.remove(uuid);
    }

    protected final void onVerify(UUID sender, int ver) {
        if (ver == Constants.PROTOCOL_VERSION) {
            if (clients.get(sender) == PacketHello.State.WAITING_FOR_REPLY) {
                clients.put(sender, PacketHello.State.COMPLETE);
                logger.logInfo("Established connection to client with uuid " + sender);
            } else {
                logger.logWarning("Received hello packet from unexpected client " + sender);
            }
        }
    }

    protected final void tickAndUpdate() {
        ArrayList<UUID> list = null;
        Iterator<Map.Entry<UUID, Counter>> iterator = pending.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Counter> entry = iterator.next();

            if (entry.getValue().update()) {
                iterator.remove();

                UUID uuid = entry.getKey();

                PacketHello.State state = clients.get(uuid);
                if (state == null) {

                    server.packetHandler.sendToClient(entry.getKey(), new PacketHello());

//                    sendMessage(entry.getKey(), new PacketHello());
                    clients.put(uuid, PacketHello.State.WAITING_FOR_REPLY);

                    if (list == null) list = new ArrayList<>();
                    list.add(uuid);
                    logger.logInfo("Begin to connect to client with uuid " + uuid);
                } else if (state != PacketHello.State.COMPLETE) {
                    clients.remove(uuid);
                    logger.logInfo("Failed to establish the connection to client with uuid " + uuid + ": Timeout");
                }
            }
        }

        if (list != null)
            list.forEach(uuid -> pending.put(uuid, new Counter(600)));
    }

}
