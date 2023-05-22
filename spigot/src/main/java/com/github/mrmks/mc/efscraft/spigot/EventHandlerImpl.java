package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;

import java.util.Map;
import java.util.UUID;

class EventHandlerImpl extends com.github.mrmks.mc.efscraft.EventHandler implements Listener {

    private final NetworkWrapper network;
    EventHandlerImpl(NetworkWrapper network, Map<UUID, PacketHello.State> clients) {
        super(clients);
        this.network = network;
    }

    @EventHandler
    public void playerLogin(PlayerJoinEvent event) {
        onLogin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void playerLogout(PlayerQuitEvent event) {
        onLogout(event.getPlayer().getUniqueId());
    }

    void tick() {
        tickAndUpdate();
    }

    @Override
    protected void sendMessage(UUID uuid, IMessage message) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) network.sendTo(player, message);
    }

    Listener channelListener() {
        return new ChannelEventHandler();
    }

    class ChannelEventHandler implements Listener {

        @EventHandler
        public void registerChannel(PlayerRegisterChannelEvent event) {
            if (event.getChannel().equals(Constants.CHANNEL_KEY))
                onLogin(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void unregisterChannel(PlayerUnregisterChannelEvent event) {
            if (event.getChannel().equals(Constants.CHANNEL_KEY))
                onLogout(event.getPlayer().getUniqueId());
        }
    }
}
