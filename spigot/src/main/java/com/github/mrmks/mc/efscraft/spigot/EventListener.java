package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.packet.PacketHello;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

class EventListener implements Listener {

    private final MessageCodecAdaptor network;
    private final Set<UUID> clients;
    private final Map<UUID, Count> waiting = new HashMap<>();
    EventListener(MessageCodecAdaptor network, Set<UUID> clients) {
        this.network = network;
        this.clients = clients;
    }

    @EventHandler
    public void playerLogin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        clients.remove(uuid);
        waiting.put(uuid, new Count());
    }

    @EventHandler
    public void playerLogout(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        clients.remove(uuid);
        waiting.remove(uuid);
    }

    void tick() {
        Iterator<Map.Entry<UUID, Count>> iterator = waiting.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Count> entry = iterator.next();
            if (--entry.getValue().count <= 0) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    network.sendTo(player, new PacketHello());
                }

                iterator.remove();
            }
        }
    }

    private static class Count {
        int count = 10;
    }

}
