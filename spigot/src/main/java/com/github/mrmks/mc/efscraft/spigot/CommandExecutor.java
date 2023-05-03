package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.EffectRegistry;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public class CommandExecutor implements TabExecutor {

    MessageCodecAdaptor wrapper;
    EffectRegistry registry;
    EffekseerCraft plugin;
    Set<UUID> clients;

    CommandExecutor(EffekseerCraft plugin, EffectRegistry registry, MessageCodecAdaptor wrapper, Set<UUID> clients) {
        this.plugin = plugin;
        this.registry = registry;
        this.wrapper = wrapper;
        this.clients = clients;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!"effek".equals(label)) return false;

        if (args.length > 0) {
            String sub = args[0];

            if ("reload".equals(sub)) {
                Runnable post;
                Consumer<CommandSender> consumer = s -> {
                    if (s instanceof Player && clients.contains(((Player) s).getUniqueId())) {
                        s.spigot().sendMessage(new TranslatableComponent("commands.effek.reload.success"));
                    } else if (s != null) {
                        s.sendMessage("Server side effect registry reload completed.");
                    }
                };
                if (sender instanceof Player) {
                    UUID uuid = ((Player) sender).getUniqueId();
                    post = () -> consumer.accept(plugin.getServer().getPlayer(uuid));
                } else {
                    post = () -> consumer.accept(plugin.getServer().getConsoleSender());
                }
                registry.reload(() -> plugin.getServer().getScheduler().runTask(plugin, post));

                return true;
            }
            else if ("play".equals(sub) || "stop".equals(sub)) {

                boolean isPlay = "play".equals(sub);

                if (args.length < 3) {
                    String msg = String.format("/effek %s <effect> <emitter> <entity> or /effek %s <effect> <emitter> <world> <x> <y> <z>", sub, sub);
                    if (isPlay) msg += " [yaw] [pitch]";
                    sender.sendMessage(ChatColor.RED + msg);
                    return true;
                }

                if (isPlay && !registry.isExist(args[1])) {
                    if (sender instanceof Player && clients.contains(((Player) sender).getUniqueId())) {
                        BaseComponent component = new TranslatableComponent("commands.effek.effect.notFound", args[1]);
                        component.setColor(net.md_5.bungee.api.ChatColor.RED);
                        sender.spigot().sendMessage(component);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Server side effect registry '" + args[1] + "' cannot be found");
                    }
                    return true;
                }

                IMessage packet;

                if (args.length > 6) {

                    World world = plugin.getServer().getWorld(args[3]);
                    if (world == null) {
                        if (sender instanceof Player && clients.contains(((Player) sender).getUniqueId())) {
                            BaseComponent component = new TranslatableComponent("commands.effek.world.notFound.name", args[3]);
                            component.setColor(net.md_5.bungee.api.ChatColor.RED);
                            sender.spigot().sendMessage(component);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Cannot find world with name " + args[3]);
                        }
                        return true;
                    }

                    double x = Double.parseDouble(args[4]);
                    double y = Double.parseDouble(args[5]);
                    double z = Double.parseDouble(args[6]);

                    if (isPlay) {
                        if (args.length > 8) {
                            float yaw = Float.parseFloat(args[7]);
                            float pitch = Float.parseFloat(args[8]);

                            packet = registry.createPlayAt(args[1], args[2], x, y, z, yaw, pitch);
                        } else {
                            packet = registry.createPlayAt(args[1], args[2], x, y, z);
                        }
                    } else {
                        packet = registry.createStop(args[1], args[2]);
                    }

                    sendToNearby(world, x, y, z, packet);
                } else {

                    Entity entity = null;

                    try {
                        UUID uuid = UUID.fromString(args[3]);
                        entity = plugin.getServer().getEntity(uuid);
                    } catch (IllegalArgumentException e) {}

                    if (entity == null) {
                        entity = plugin.getServer().getPlayer(args[3]);
                    }

                    if (entity == null) {
                        BaseComponent component = new TranslatableComponent("commands.generic.entity.notFound", args[3]);
                        component.setColor(net.md_5.bungee.api.ChatColor.RED);
                        sender.spigot().sendMessage(component);
                        return true;
                    }

                    Location location = entity.getLocation();

                    if (isPlay) {
                        packet = registry.createPlayWith(args[1], args[2], entity.getEntityId());
                    } else {
                        packet = registry.createStop(args[1], args[2]);
                    }

                    sendToNearby(entity.getWorld(), location.getX(), location.getY(), location.getZ(), packet);
                }

                return true;
            }
            else if ("clear".equals(sub)) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/effek clear <player>");
                    return true;
                }

                Player player = plugin.getServer().getPlayer(args[1]);

                if (player == null) {
                    try {
                        UUID uuid = UUID.fromString(args[1]);
                        player = plugin.getServer().getPlayer(uuid);
                    } catch (IllegalArgumentException e) {}
                }

                if (player == null) {
//                    sender.sendMessage("We can't find that player");
                    BaseComponent component = new TranslatableComponent("commands.generic.player.notFound", args[1]);
                    component.setColor(net.md_5.bungee.api.ChatColor.RED);
                    sender.spigot().sendMessage(component);
                    return true;
                }

                if (clients.contains(player.getUniqueId()))
                    wrapper.sendTo(player, new SPacketClear());

                return true;
            } else if ("version".equals(sub)) {
                if (sender instanceof Player && clients.contains(((Player) sender).getUniqueId())) {
                    BaseComponent component = new TranslatableComponent("commands.effek.version.display",
                            plugin.getDescription().getVersion(), String.valueOf(Constants.PROTOCOL_VERSION), "bukkit");

                    sender.spigot().sendMessage(component);
                } else {
                    sender.sendMessage(String.format("version: %s, protocol: %d, port: bukkit", plugin.getDescription().getVersion(), Constants.PROTOCOL_VERSION));
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    private void sendToNearby(World world, double x, double y, double z, IMessage packet) {

        long chunkX = Math.round(x - 0.5) >> 4, chunkZ = Math.round(z - 0.5) >> 4;

        for (Player player : world.getPlayers()) {
            if (player != null && player.isValid() && clients.contains(player.getUniqueId())) {

                Location loc = player.getLocation();
                long cx = loc.getBlockX() >> 4, cz = loc.getBlockZ() >> 4;

                if (Math.abs(cx - chunkX) <= 10 && Math.abs(cz - chunkZ) <= 10) {
                    wrapper.sendTo(player, packet);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!"effek".equals(label)) return Collections.emptyList();

        if (args.length == 1) return filterCompletes(args, "play", "stop", "clear", "reload", "version");
        else {
            String sub = args[0];
            if ("play".equals(sub) || "stop".equals(sub)) {
                if (args.length == 2) return filterCompletes(args, registry.keySets());
                else if (args.length == 4) return null;
                else return Collections.emptyList();
            }
            else if ("clear".equals(sub)) {
                if (args.length == 2) return null;
                else return Collections.emptyList();
            }
            else return Collections.emptyList();
        }
    }

    private List<String> filterCompletes(String[] args, String... completes) {
        return filterCompletes(args, Arrays.asList(completes));
    }

    private List<String> filterCompletes(String[] args, Collection<String> completes) {
        String last = args[args.length - 1];
        ArrayList<String> list = new ArrayList<>();
        for (String str : completes)
            if (str.startsWith(last)) list.add(str);

        list.sort(String::compareTo);

        return list;
    }
}
