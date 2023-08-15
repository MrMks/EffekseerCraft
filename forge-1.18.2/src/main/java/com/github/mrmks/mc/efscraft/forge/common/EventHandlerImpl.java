package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.server.EfsServerEventHandler;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;

public class EventHandlerImpl extends EfsServerEventHandler {

    private final NetworkWrapper wrapper;
    EventHandlerImpl(NetworkWrapper wrapper, Map<UUID, PacketHello.State> clients, LogAdaptor logger) {
        super(clients, logger);
        this.wrapper = wrapper;
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        onLogin(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        onLogout(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (ServerLifecycleHooks.getCurrentServer() != null)
                tickAndUpdate();
        }
    }

//    @Override
    protected void sendMessage(UUID uuid, NetworkPacket message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            PlayerList list = server.getPlayerList();
            Player player = list.getPlayer(uuid);

            if (player != null) wrapper.sendTo(player, message);
        }
    }
}
