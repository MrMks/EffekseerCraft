package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.EventHandler;
import com.github.mrmks.mc.efscraft.ILogAdaptor;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

class EventHandlerImpl extends EventHandler {

    private final NetworkWrapper wrapper;

    EventHandlerImpl(NetworkWrapper wrapper, Map<UUID, PacketHello.State> clients, ILogAdaptor adaptor) {
        super(clients, adaptor);
        this.wrapper = wrapper;
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        onLogin(event.player.getPersistentID());
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        onLogout(event.player.getPersistentID());
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;

            tickAndUpdate();
        }
    }

    @Override
    protected void sendMessage(UUID uuid, IMessage message) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        PlayerList list = server.getPlayerList();

        EntityPlayer player = list.getPlayerByUUID(uuid);

        wrapper.sendTo(player, message);
    }

}
