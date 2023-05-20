package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.EventHandler;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;

import java.util.Set;
import java.util.UUID;

class EventHandlerImpl extends EventHandler {

    private final NetworkWrapper wrapper;
    EventHandlerImpl(Set<UUID> clients, NetworkWrapper wrapper) {
        super(clients);
        this.wrapper = wrapper;
    }

    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        onLogin(event.getPlayer().getUUID());
    }

    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        onLogout(event.getPlayer().getUUID());
    }

    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
            if (server != null) tickAndUpdate();
        }
    }

    @Override
    protected void sendMessage(UUID uuid, IMessage message) {
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server != null) {
            PlayerList list = server.getPlayerList();
            PlayerEntity player = list.getPlayer(uuid);

            if (player != null) wrapper.sendTo(player, message);
        }
    }
}
