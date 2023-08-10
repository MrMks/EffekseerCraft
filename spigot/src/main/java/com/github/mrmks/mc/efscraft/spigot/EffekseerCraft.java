package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.*;
import com.github.mrmks.mc.efscraft.common.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.common.event.EfsTickEvent;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChannelEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
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

        LogAdaptor logger = LogAdaptor.of(getLogger());
        EfsAdaptorImpl adaptor = new EfsAdaptorImpl(this);

        EfsServer<Entity, Player, World, CommandSender, ByteArrayDataOutput, ByteArrayDataInput, Server> server = new EfsServer<>(
                adaptor,
                logger,
                Collections.singletonList(new File(getDataFolder(), "effects.json")),
                EfsServerEnv.BUKKIT,
                this.getDescription().getVersion(),
                true
        );

        // network
        Messenger messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        messenger.registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, (channel, player, data) -> {
            try {
                server.receivePacket(player, ByteStreams.newDataInput(data));
            } catch (IOException e) {
                logger.logWarning("Unable to handle a client packet", e);
            }
        });

        // event
        EventHandler eventHandler = new EventHandler(server);
        getServer().getPluginManager().registerEvents(eventHandler, this);
        // and tick
        getServer().getScheduler().runTaskTimer(this, () -> server.receiveEvent(new EfsTickEvent()), 0, 0);

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

        if (true)
            return;

        NetworkWrapper network = new NetworkWrapper(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, network);

        Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();

        network.register(PacketHello.class, new PacketHello.ServerHandler(clients, logger));

//        PluginCommand command = getCommand("effek");
        if (command != null) command.setExecutor(new CommandAdaptor(this, network, clients, localize));

        EventHandlerImpl listener = new EventHandlerImpl(this, network, clients, logger);
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

    static class EventHandler implements Listener {
        EfsServer<?,?,?,?,?,?,?> server;
        EventHandler(EfsServer<?,?,?,?,?,?,?> server) { this.server = server; }

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

        EfsServer<?,?,?,CommandSender,?,?,Server> server;
        Localize localize;

        CommandHandler(EfsServer<?,?,?,CommandSender,?,?,Server> server, Localize localize) {
            this.server = server;
            this.localize = localize;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            try {
                server.executeCommands(label, args, sender, Bukkit.getServer());
            } catch (EfsCommandHandler.CommandException e) {
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
