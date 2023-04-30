package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.EffectRegistry;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CommandExecutor implements TabExecutor {

    MessageCodecAdaptor wrapper;
    EffectRegistry registry;
    EfsCraft plugin;

    CommandExecutor(EfsCraft plugin, EffectRegistry registry, MessageCodecAdaptor wrapper) {
        this.plugin = plugin;
        this.registry = registry;
        this.wrapper = wrapper;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!"effek".equals(label)) return false;

        if (args.length > 0) {
            String sub = args[0];

            if ("reload".equals(sub)) {
                Runnable post;
                Consumer<CommandSender> consumer = s -> {
                    if (s != null) s.sendMessage("EfsCraft effect registry reload completed.");
                };
                if (sender instanceof Player) {
                    UUID uuid = ((Player) sender).getUniqueId();
                    post = () -> consumer.accept(Bukkit.getPlayer(uuid));
                } else {
                    post = () -> consumer.accept(Bukkit.getConsoleSender());
                }
                registry.reload(() -> Bukkit.getScheduler().runTask(plugin, post));

                return true;
            }
            else if ("play".equals(sub) || "stop".equals(sub)) {
                if (args.length < 2) {
                    sender.sendMessage("You should input which effect to play/stop");
                    return true;
                } else if (args.length < 3) {
                    sender.sendMessage("You should input which player you want play/stop at, or give a position(world, x, y, z).");
                    return true;
                }

                if (!registry.isExist(args[1])) {
                    sender.sendMessage("Such a effect name isn't exist, please check your registry file.");
                    return true;
                }

                IMessage packet;

                World world;
                double x, y, z;
                if (args.length > 5) {

                    world = Bukkit.getWorld(args[2]);
                    if (world == null) {
                        sender.sendMessage("We can't find that world");
                        return true;
                    }

                    x = Float.parseFloat(args[3]);
                    y = Float.parseFloat(args[4]);
                    z = Float.parseFloat(args[5]);

                    packet = registry.createPlayAt(args[1], null, x, y, z);

                } else {

                    Player player = Bukkit.getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage("Can't find target player");
                        return true;
                    }

                    world = player.getWorld();
                    Location location = player.getLocation();

                    x = location.getX();
                    y = location.getY();
                    z = location.getZ();

                    if ("play".equals(sub)) {
                        packet = registry.createPlayWith(args[1], null, player.getUniqueId());
                    } else {
                        packet = registry.createStop(args[1]);
                    }
                }

                sendToNearby(world, x, y, z, packet);
            }
            else if ("clear".equals(sub)) {
                if (args.length < 2) {
                    sender.sendMessage("You must specific which player to clear effects");
                    return true;
                }

                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage("We can't find that player");
                    return true;
                }

                wrapper.sendTo(player, new SPacketClear());
            } else {
                return false;
            }
        } else {
            sender.sendMessage("EfsCraft(v. " + plugin.getDescription().getVersion() + ") running on Bukkit");
            return true;
        }

        return false;
    }

    private void sendToNearby(World world, double x, double y, double z, IMessage packet) {
        long chunkX = ((long) x) >>> 5, chunkZ = ((long) z) >>> 5;
        List<Player> players = world.getPlayers();
        for (Player player : players) {
            if (player != null && player.isValid()) {
                Location loc = player.getLocation();
                long cx = loc.getChunk().getX(), cz = loc.getChunk().getZ();

                cx = Math.abs(cx - chunkX); cz = Math.abs(cz - chunkZ);

                if (cx <= 10 && cz <= 10) {
                    wrapper.sendTo(player, packet);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
