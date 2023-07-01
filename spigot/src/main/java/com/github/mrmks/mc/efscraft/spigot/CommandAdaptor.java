package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.CommandHandler;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CommandAdaptor implements TabExecutor, CommandHandler.Adaptor<Entity, Player, Server, CommandSender, World> {

    private final Plugin plugin;
    private final NetworkWrapper wrapper;
    private final Localize localize;
    private final CommandHandler<Entity, Player, Server, CommandSender, World> handler;

    @Override
    public boolean hasPermission(Server server, CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    public UUID getClientUUID(Player sender) {
        return sender.getUniqueId();
    }

    @Override
    public void sendPacketTo(Server server, Player player, NetworkPacket message) {
        wrapper.sendTo(player, message);
    }

    @Override
    public Player findPlayer(Server server, CommandSender sender, String toFound) throws CommandHandler.CommandException {
        Player player = server.getPlayer(toFound);
        if (player == null) {
            try {
                UUID uuid = UUID.fromString(toFound);
                player = server.getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {}
        }

        if (player == null) throw new CommandHandler.CommandException("commands.generic.player.notFound", toFound) {};

        return player;
    }

    @Override
    public Entity findEntity(Server server, CommandSender sender, String toFound) throws CommandHandler.CommandException {
        Entity entity = null;
        try {
            UUID uuid = UUID.fromString(toFound);
            entity = server.getEntity(uuid);
            if (entity == null) {
                entity = server.getPlayer(uuid);
            }
        } catch (IllegalArgumentException ignored) {}

        if (entity == null) {
            entity = server.getPlayer(toFound);
        }

        if (entity == null) throw new CommandHandler.CommandException("commands.generic.entity.notFound", toFound) {};

        return entity;
    }

    @Override
    public Collection<Player> getPlayersInWorld(Server server, CommandSender sender, World world) {
        return world.getPlayers();
    }

    @Override
    public float[] getSenderPosAngle(CommandSender sender) {
        return sender instanceof Entity ? getEntityPosAngle((Entity) sender) : new float[5];
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getEntityId();
    }

    @Override
    public float[] getEntityPosAngle(Entity entity) {
        Location loc = entity.getLocation();
        return new float[] {(float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), loc.getYaw(), loc.getPitch()};
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getWorld();
    }

    @Override
    public World findWorld(Server server, CommandSender sender, String str) throws CommandHandler.CommandException {
        return server.getWorld(str);
    }

    @Override
    public Collection<String> completePlayers(Server server) {
        return server.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @Override
    public Collection<String> completeWorlds(Server server) {
        return server.getWorlds().stream().map(World::getName).collect(Collectors.toList());
    }

    @Override
    public int getViewDistance(World world) {
        return plugin.getServer().getViewDistance();
    }

    @Override
    public void sendMessage(CommandSender sender, String msg, Object[] objects, boolean schedule) {
        if (schedule) {
            sender.getServer().getScheduler().runTaskLater(plugin, () -> sendMessage0(sender, msg, objects, false), 1L);
        } else {
            sendMessage0(sender, msg, objects, false);
        }
    }

    private void sendMessage0(CommandSender sender, String msg, Object[] objects, boolean exception) {
        if (sender instanceof Entity) {
            Entity entity = (Entity) sender;
            if (entity.isValid()) {
                BaseComponent component = new TranslatableComponent(msg, objects);
                if (exception) component.setColor(ChatColor.RED);
                entity.spigot().sendMessage(component);
            } else {
                sendTranslated(sender, msg, objects, exception);
            }
        } else {
            sendTranslated(sender, msg, objects, exception);
        }
    }

    private void sendTranslated(CommandSender sender, String msg, Object[] objects, boolean exception) {
        msg = localize.translate(msg, objects);
        if (exception) msg = org.bukkit.ChatColor.RED + msg;
        sender.sendMessage(msg);
    }

    // ====== ======

    CommandAdaptor(Plugin plugin, NetworkWrapper wrapper, Map<UUID, PacketHello.State> clients, Localize localize) {
        this.wrapper = wrapper;
        this.plugin = plugin;
        this.localize = localize;

        this.handler = new CommandHandler<>(this, new File(plugin.getDataFolder(), "effects.json"), "spigot", plugin.getDescription().getVersion(), clients);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            handler.dispatchExecute(label, args, plugin.getServer(), sender);
        } catch (CommandHandler.CommandException e) {
            sendMessage0(sender, e.getMessage(), e.getParams(), true);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return handler.dispatchComplete(label, args, plugin.getServer(), sender);
    }

}
