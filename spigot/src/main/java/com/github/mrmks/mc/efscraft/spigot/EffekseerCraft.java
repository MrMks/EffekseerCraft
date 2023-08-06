package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import com.google.common.collect.ImmutableMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * We will not provide api for this, please use commands instead;
 */
public class EffekseerCraft extends JavaPlugin {

    private final boolean forgeDetected;

    public EffekseerCraft() {
        boolean flag = false;
        try {
            // we will not work together with mods, it is not necessary.
            Class.forName("com.github.mrmks.mc.efscraft.forge.EffekseerCraft");
            flag = true;
        } catch (ClassNotFoundException e) {
            // we are not working with mods;
        }
        forgeDetected = flag;
    }

    @Override
    public void onLoad() {
        if (forgeDetected) {
            PluginDescriptionFile desc = getDescription();
            Map<String, Map<String, Object>> commands = desc.getCommands();
            commands = new HashMap<>(commands);
            commands.remove("effek");
            commands = ImmutableMap.copyOf(commands);

            try {
                Field field = desc.getClass().getDeclaredField("commands");
                field.setAccessible(true);
                field.set(desc, commands);
            } catch (Throwable tr) {
                tr.printStackTrace();
            }

            return;
        }
    }

    @Override
    public void onEnable() {
        if (forgeDetected) {
            getServer().getScheduler().runTaskLater(this, () -> {
                getLogger().warning("Found mod side efscraft, plugin side will disable itself.");
                getServer().getPluginManager().disablePlugin(this);
            }, 1L);

            return;
        }

        LogAdaptor adaptor = LogAdaptor.of(getLogger());
        NetworkWrapper network = new NetworkWrapper(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, network);

        Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();

        network.register(PacketHello.class, new PacketHello.ServerHandler(clients, adaptor));

        Localize localize = new Localize();
        try (InputStream stream = getResource("lang/en_us.lang")) {
            localize.onLoad(stream);
        } catch (IOException e) {
            getLogger().warning("Unable to load lang/en_us.lang");
        }

        PluginCommand command = getCommand("effek");
        if (command != null) command.setExecutor(new CommandAdaptor(this, network, clients, localize));

        EventHandlerImpl listener = new EventHandlerImpl(this, network, clients, adaptor);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, listener::tick, 0, 0);
        try {
            getServer().getPluginManager().registerEvents(listener.channelListener(), this);
        } catch (NoClassDefFoundError error) {
            getServer().getPluginManager().registerEvents(listener.loginListener(), this);
        }
    }

    @Override
    public void onDisable() {
        if (forgeDetected) return;

        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);

        PluginCommand command = getCommand("effek");
        if (command != null) command.setExecutor(null);
    }
}
