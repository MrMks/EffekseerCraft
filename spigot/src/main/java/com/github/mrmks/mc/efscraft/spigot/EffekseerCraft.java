package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.github.mrmks.mc.efscraft.server.EfsServerEnv;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.server.event.EfsServerEvent;
import com.google.common.collect.ImmutableMap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        LogAdaptor logger = LogAdaptor.of(getLogger());
        EfsAdaptorImpl adaptor = new EfsAdaptorImpl(this);

        EfsServerImpl server = new EfsServerImpl(
                adaptor,
                logger,
                EfsServerEnv.BUKKIT,
                this.getDescription().getVersion(),
                true
        );

        {
            List<File> files = Collections.singletonList(new File(getDataFolder(), "effects.json"));
            File keys = new File(getDataFolder(), "decrypts.json");
            server.receiveEvent(new EfsServerEvent.Start<>(getServer(), files, keys));
        }

        // network
        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        messenger.registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, (channel, player, data) -> {
            try {
                server.receivePacket(getServer(), player, new ByteArrayInputStream(data));
            } catch (IOException e) {
                logger.logWarning("Unable to handle a client packet", e);
            }
        });

        // event
        EventHandler eventHandler = new EventHandler(server);
        getServer().getPluginManager().registerEvents(eventHandler, this);
        // and tick
        getServer().getScheduler().runTaskTimer(this, () -> server.receiveEvent(new EfsServerEvent.Tick<>(getServer())), 0, 0);

        // localize
        Localize localize = new Localize();
        try (InputStream stream = getResource("lang/en_us.lang")) {
            localize.onLoad(stream);
        } catch (IOException e) {
            getLogger().warning("Unable to load lang/en_us.lang");
        }

        // commands
        PluginCommand command = getCommand("effek");
        if (command != null) {
            CommandHandler commandHandler = new CommandHandler(server, localize);
            command.setExecutor(commandHandler);
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

    private static class EfsServerImpl extends EfsServer<Server, World, Entity, Player, CommandSender, ByteArrayOutputStream> {
        public EfsServerImpl(EfsAdaptorImpl adaptor, LogAdaptor logger, EfsServerEnv env, String implVer, boolean autoReply) {
            super(adaptor, logger, env, implVer, autoReply);
        }
    }

    static class EventHandler implements Listener {
        EfsServer<?,?,?,?,?,?> server;
        EventHandler(EfsServer<?,?,?,?,?,?> server) { this.server = server; }

        @org.bukkit.event.EventHandler
        public void handleLogin(PlayerRegisterChannelEvent event) {
            if (Constants.CHANNEL_KEY.equals(event.getChannel()))
                server.receiveEvent(new EfsPlayerEvent.Join(event.getPlayer().getUniqueId()));
        }

        @org.bukkit.event.EventHandler
        public void handleLeave(PlayerQuitEvent event) {
            server.receiveEvent(new EfsPlayerEvent.Leave(event.getPlayer().getUniqueId()));
        }
    }

    static class CommandHandler implements TabExecutor {

        EfsServer<Server,?,?,?,CommandSender,?> server;
        Localize localize;

        CommandHandler(EfsServer<Server,?,?,?,CommandSender,?> server, Localize localize) {
            this.server = server;
            this.localize = localize;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            try {
                server.executeCommands(label, args, sender, Bukkit.getServer());
            } catch (EfsServerCommandHandler.CommandException e) {
                if (sender instanceof Player) {
                    TranslatableComponent component = new TranslatableComponent(e.getMessage(), e.getParams());
                    component.setColor(ChatColor.RED);
                    sender.spigot().sendMessage(component);
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.RED + localize.translate(e.getMessage(), e.getParams()));
                }
            }

            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return server.completeCommands(alias, args, sender, Bukkit.getServer());
        }
    }
}
