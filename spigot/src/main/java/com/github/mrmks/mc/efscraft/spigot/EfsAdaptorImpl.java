package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.EfsCommandHandler;
import com.github.mrmks.mc.efscraft.common.IEfsServerAdaptor;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class EfsAdaptorImpl implements IEfsServerAdaptor<Server, World, Entity, Player, CommandSender, ByteArrayDataOutput, ByteArrayDataInput> {

    private final Plugin plugin;
    EfsAdaptorImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(Server server, CommandSender sender, String permissionNode) {
        return sender.hasPermission(permissionNode);
    }

    @Override
    public Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    public Entity findEntity(Server server, CommandSender sender, String toFind) {
        Entity entity = null;
        try {
            UUID uuid = UUID.fromString(toFind);
            entity = server.getEntity(uuid);
            if (entity == null)
                entity = server.getPlayer(uuid);
        } catch (IllegalArgumentException e) {}

        if (entity == null)
            entity = server.getPlayer(toFind);

        return entity;
    }

    @Override
    public Player findPlayer(Server server, CommandSender sender, String toFind) {
        Player player = null;

        try {
            UUID uuid = UUID.fromString(toFind);
            player = server.getPlayer(uuid);
        } catch (IllegalArgumentException e) {}

        if (player == null)
            player = server.getPlayer(toFind);

        return player;
    }

    @Override
    public Entity getEntity(Server server, World world, UUID uuid) {
        Entity entity = server.getEntity(uuid);

        return entity != null && entity.getWorld().equals(world) ? entity : null;
    }

    @Override
    public Player getPlayerEntity(Entity entity) {
        return entity instanceof Player ? (Player) entity : null;
    }

    @Override
    public Entity getEntitySender(CommandSender sender) {
        return sender instanceof Entity ? (Entity) sender : null;
    }

    @Override
    public World getWorld(Server server, String world) throws EfsCommandHandler.CommandException {
        return null;
    }

    @Override
    public World getWorld(Server server, CommandSender sender, String toFind) throws EfsCommandHandler.CommandException {
        World world = null;
        try {
            UUID uuid = UUID.fromString(toFind);
            world = server.getWorld(uuid);
        } catch (IllegalArgumentException e) {}

        if (world == null)
            world = server.getWorld(toFind);

        return world;
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getEntityId();
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getUniqueId();
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getWorld();
    }

    @Override
    public Vec3f getEntityPos(Entity entity) {
        Location location = entity.getLocation();

        return new Vec3f(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public Vec2f getEntityAngle(Entity entity) {
        Location location = entity.getLocation();

        return new Vec2f(location.getYaw(), location.getPitch());
    }

    @Override
    public String getPlayerName(Player player) {
        return player.getName();
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    @Override
    public int getWorldViewDistance(World world) {
        return Bukkit.getViewDistance();
    }

    @Override
    public Vec3f getSenderPos(CommandSender sender) {
        if (sender instanceof BlockCommandSender) {
            Block block = ((BlockCommandSender) sender).getBlock();
            return new Vec3f(block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
        } else if (sender instanceof Entity) {
            Location location = ((Entity) sender).getLocation();

            return new Vec3f(location.getX(), location.getY(), location.getZ());
        } else {
            return new Vec3f(0, 0, 0);
        }
    }

    @Override
    public List<World> getWorlds(Server server) {
        return server.getWorlds();
    }

    @Override
    public List<World> getWorlds(Server server, CommandSender sender) {
        return server.getWorlds();
    }

    @Override
    public List<Player> getPlayersInWorld(World world) {
        return world.getPlayers();
    }

    @Override
    public List<Player> getPlayersInServer(Server server) {
        return new ArrayList<>(server.getOnlinePlayers());
    }

    @Override
    public List<Player> getPlayersInServer(Server server, CommandSender sender) {
        return new ArrayList<>(server.getOnlinePlayers());
    }

    @Override
    public ByteArrayDataOutput createPacket() {
        return ByteStreams.newDataOutput();
    }

    @Override
    public void sendPacket(Collection<Player> players, Predicate<Player> test, ByteArrayDataOutput output) {
        byte[] data = output.toByteArray();
        players.stream()
                .filter(test)
                .forEach(it -> it.sendPluginMessage(plugin, Constants.CHANNEL_KEY, data));
    }

    @Override
    public void sendPacket(Server server, Collection<Player> players, Predicate<Player> test, ByteArrayDataOutput output) {
        byte[] data = output.toByteArray();
        players.stream()
                .filter(test)
                .forEach(it -> it.sendPluginMessage(plugin, Constants.CHANNEL_KEY, data));
    }

    @Override
    public void sendMessage(Server server, CommandSender sender, String msg, Object[] args, boolean scheduled) {
        if (scheduled)
            Bukkit.getScheduler().runTask(plugin, () -> sendMessage(server, sender, msg, args, false));
        else {
            sender.spigot().sendMessage(new TranslatableComponent(msg, args));
        }
    }
}
