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
    private final Set<UUID> clients;
    private final Map<UUID, Counter> pending = new HashMap<>();

    protected EventHandler(Set<UUID> clients) {
        this.clients = clients;
    }

    protected final void onLogin(UUID uuid) {
        pending.computeIfAbsent(uuid, it -> new Counter(10));
    }

    protected final void onLogout(UUID uuid) {
        pending.remove(uuid);
        clients.remove(uuid);
    }

    protected final void tickAndUpdate() {
        Iterator<Map.Entry<UUID, Counter>> iterator = pending.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Counter> entry = iterator.next();

            if (entry.getValue().update()) {
                iterator.remove();

                sendMessage(entry.getKey(), new PacketHello());
            }
        }
    }

    protected abstract void sendMessage(UUID uuid, IMessage message);
}
