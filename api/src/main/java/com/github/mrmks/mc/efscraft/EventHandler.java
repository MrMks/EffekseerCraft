package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.PacketHello;

import java.util.*;

public abstract class EventHandler {

    private static class Counter {
        int count;
        Counter(int init) {
            this.count = init;
        }

        boolean update() {
            return --count < 0;
        }
    }
    private final Map<UUID, PacketHello.State> clients;
    private final Map<UUID, Counter> pending = new HashMap<>();

    protected EventHandler(Map<UUID, PacketHello.State> clients) {
        this.clients = clients;
    }

    protected final void onLogin(UUID uuid) {
        if (!clients.containsKey(uuid))
            pending.computeIfAbsent(uuid, it -> new Counter(10));
    }

    protected final void onLogout(UUID uuid) {
        pending.remove(uuid);
        clients.remove(uuid);
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
                    sendMessage(entry.getKey(), new PacketHello());
                    clients.put(uuid, PacketHello.State.WAITING_FOR_REPLY);

                    if (list == null) list = new ArrayList<>();
                    list.add(uuid);
                } else if (state != PacketHello.State.COMPLETE) {
                    clients.remove(uuid);
                }
            }
        }

        if (list != null)
            list.forEach(uuid -> pending.put(uuid, new Counter(100)));
    }

    protected abstract void sendMessage(UUID uuid, IMessage message);
}
