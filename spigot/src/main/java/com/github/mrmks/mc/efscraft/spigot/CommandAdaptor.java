package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.CommandHandler;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import net.md_5.bungee.api.chat.TranslatableComponent;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandAdaptor implements TabExecutor, CommandHandler.Adaptor<Entity, Player, Server, CommandSender, World> {

    private final Plugin plugin;
    private final Set<UUID> clients;
    private final NetworkWrapper wrapper;
    private final Localize localize;
    private final CommandHandler<Entity, Player, Server, CommandSender, World> handler;

    @Override
    public boolean hasPermission(Server server, CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    public boolean isClientValid(Player sender) {
        return clients.contains(sender.getUniqueId());
    }

    @Override
    public void sendPacketTo(Server server, Player player, IMessage message) {
        wrapper.sendTo(player, message);
    }

    @Override
    public Player findPlayer(Server server, CommandSender sender, String toFound) throws CommandHandler.CommandException {
        Player player = server.getPlayer(toFound);
        if (player == null) try {
            UUID uuid = UUID.fromString(toFound);
            player = server.getPlayer(uuid);
        } catch (IllegalArgumentException e) {}

        if (player == null) throw new CommandHandler.CommandException("commands.generic.player.notFound", toFound) {};

        return player;
    }

    @Override
    public Entity findEntity(Server server, CommandSender sender, String toFound) throws CommandHandler.CommandException {
        Entity entity = null;
        try {
            UUID uuid = UUID.fromString(toFound);
            entity = server.getEntity(uuid);
        } catch (IllegalArgumentException e) {}

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
    public void sendMessage(CommandSender sender, String msg, Object[] objects, boolean schedule) {
        if (schedule) {
            sender.getServer().getScheduler().runTaskLater(plugin, () -> sendMessage(sender, msg, objects, false), 1L);
        } else {
            if (sender instanceof Entity) {
                Entity entity = (Entity) sender;
                if (entity.isValid()) {
                    entity.spigot().sendMessage(new TranslatableComponent(msg, objects));
                } else {
                    sendTranslated(sender, msg, objects);
                }
            } else {
                sendTranslated(sender, msg, objects);
            }
        }
    }

    private void sendTranslated(CommandSender sender, String msg, Object[] objects) {
        sender.sendMessage(localize.translate(msg, objects));
    }

    // ====== ======

    CommandAdaptor(Plugin plugin, NetworkWrapper wrapper, Set<UUID> clients, Localize localize) {
        this.clients = clients;
        this.wrapper = wrapper;
        this.plugin = plugin;
        this.localize = localize;

        this.handler = new CommandHandler<>(this, new File(plugin.getDataFolder(), "effects.json"), "spigot", plugin.getDescription().getVersion());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            handler.dispatchExecute(label, args, plugin.getServer(), sender);
        } catch (CommandHandler.CommandException e) {
            sendMessage(sender, e.getMessage(), e.getParams(), false);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return handler.dispatchComplete(label, args, plugin.getServer(), sender);
    }

}
