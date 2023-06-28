package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class EventHandlerImpl extends com.github.mrmks.mc.efscraft.common.EventHandler implements Listener {

    private final NetworkWrapper network;
    private final Plugin plugin;
    EventHandlerImpl(Plugin plugin, NetworkWrapper network, Map<UUID, PacketHello.State> clients, ILogAdaptor adaptor) {
        super(clients, adaptor);
        this.plugin = plugin;
        this.network = network;
    }

    @EventHandler
    public void playerLogout(PlayerQuitEvent event) {
        onLogout(event.getPlayer().getUniqueId());
    }

    void tick() {
        tickAndUpdate();
    }

    @Override
    protected void sendMessage(UUID uuid, NetworkPacket message) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) network.sendTo(player, message);
    }

    Listener channelListener() {
        return new ChannelEventHandler();
    }

    Listener loginListener() {
        return new LoginEventHandler();
    }

    class ChannelEventHandler implements Listener {

        private final Set<UUID> sets = Collections.newSetFromMap(new ConcurrentHashMap<>());

        private void doLogin(UUID uuid) {
            if (sets.remove(uuid)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> onLogin(uuid));
            } else {
                sets.add(uuid);
            }
        }

        @EventHandler
        public void registerChannel(PlayerRegisterChannelEvent event) {
            if (event.getChannel().equals(Constants.CHANNEL_KEY))
                doLogin(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void playerLogin(PlayerJoinEvent event) {
            doLogin(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void unregisterChannel(PlayerUnregisterChannelEvent event) {
            if (event.getChannel().equals(Constants.CHANNEL_KEY))
                onLogout(event.getPlayer().getUniqueId());
        }
    }

    class LoginEventHandler implements Listener {
        @EventHandler
        public void playerLogin(PlayerJoinEvent event) {
            onLogin(event.getPlayer().getUniqueId());
        }

    }
}
