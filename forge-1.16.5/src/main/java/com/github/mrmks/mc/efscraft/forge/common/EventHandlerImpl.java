package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.EventHandler;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;

class EventHandlerImpl extends EventHandler {

    private final NetworkWrapper wrapper;
    EventHandlerImpl(NetworkWrapper wrapper, Map<UUID, PacketHello.State> clients) {
        super(clients);
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

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) tickAndUpdate();
        }
    }

    @Override
    protected void sendMessage(UUID uuid, IMessage message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            PlayerList list = server.getPlayerList();
            PlayerEntity player = list.getPlayer(uuid);

            if (player != null) wrapper.sendTo(player, message);
        }
    }
}
