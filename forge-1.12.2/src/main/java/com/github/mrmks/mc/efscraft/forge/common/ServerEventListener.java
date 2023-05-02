package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.packet.PacketHello;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

class ServerEventListener {
    private final Map<UUID, CountDown> counter = new HashMap<>();
    private final CommonProxy proxy;

    ServerEventListener(CommonProxy proxy) {
        this.proxy = proxy;
    }

    @SubscribeEvent
    public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        UUID uuid = event.player.getPersistentID();
        counter.put(uuid, new CountDown(10));
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.player.getPersistentID();
        counter.remove(uuid);

        proxy.logoutClient(uuid);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) return;
            PlayerList list = server.getPlayerList();

            Iterator<Map.Entry<UUID, CountDown>> iterator = counter.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, CountDown> entry = iterator.next();
                if (entry.getValue().update()) {
                    iterator.remove();
                    EntityPlayer player = list.getPlayerByUUID(entry.getKey());

                    proxy.getNetwork().sendTo(new PacketHello(), player);
                }
            }
        }
    }

    private static class CountDown {
        int current;

        CountDown(int init) {
            current = init;
        }

        boolean update() {
            current--;
            return current <= 0;
        }
    }
}
