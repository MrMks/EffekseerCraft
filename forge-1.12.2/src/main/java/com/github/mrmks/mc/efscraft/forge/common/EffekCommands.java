package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.EffectRegistry;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import com.github.mrmks.mc.efscraft.packet.SPacketStop;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

class EffekCommands extends CommandTreeBase {

    private final NetworkWrapper wrapper;
    private final EffectRegistry registry;
    private final String version;
    private final Predicate<UUID> check;

    EffekCommands(NetworkWrapper wrapper, EffectRegistry registry, String modVersion, Predicate<UUID> compCheck) {
        this.wrapper = wrapper;
        this.registry = registry;
        this.version = modVersion;
        this.check = compCheck;

        addSubcommand(new Adaptor("reload", this::executeReload));
        addSubcommand(new Adaptor("play", this::executePlay, this::completePlay));
        addSubcommand(new Adaptor("stop", this::executeStop, this::completeStop));
        addSubcommand(new Adaptor("clear", this::executeClear, this::completeClear));
        addSubcommand(new Adaptor("version", this::executeVersion));
    }

    @Override
    @Nonnull
    public String getName() {
        return "effek";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "commands.effek.usage";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, "efscraft.command");
        }
        return true;
    }

    private void sendAtPos(IMessage message, World world, double x, double y, double z) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(x, y, z));
        List<EntityPlayer> players = world.playerEntities;
        for (EntityPlayer player : players) {

            if (!check.test(player.getPersistentID())) continue;

            BlockPos p = new BlockPos(player.posX, player.posY, player.posZ);
            ChunkPos cp = new ChunkPos(p);

            if (Math.abs(cp.x - chunkPos.x) <= 10 && Math.abs(cp.z - chunkPos.z) <= 10) {
                wrapper.sendTo(message, player);
            }
        }
    }

    private World getWorld(String arg) throws CommandException {
        int dim = parseInt(arg);
        World world = DimensionManager.getWorld(dim);
        if (world == null) {
            throw new WorldNotFountException(dim);
        } else {
            return world;
        }
    }

    void executeReload(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        WeakReference<ICommandSender> senderRef = new WeakReference<>(sender);
        registry.reload(() -> server.addScheduledTask(() -> {
            ICommandSender s = senderRef.get();
            if (s != null) {
                s.sendMessage(new TextComponentTranslation("commands.effek.reload.success"));
            }
        }));
    }

    void executePlay(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length < 3)
        {
            throw new WrongUsageException("commands.effek.play.usage");
        }
        else
        {
            String effect = args[0], emitter = args[1];
            if (!registry.isExist(effect))
            {
                throw new EffectNotFoundException(effect);
            }
            else
            {
                IMessage message;

                if (args.length > 5) {
                    World world = getWorld(args[2]);
                    double x = parseDouble(args[3], -30000000, 30000000);
                    double y = parseDouble(args[4], -30000000, 30000000);
                    double z = parseDouble(args[5], -30000000, 30000000);

                    if (args.length > 7) {
                        double yaw = parseDouble(args[6], -180, 180);
                        double pitch = parseDouble(args[7], -90, 90);
                        message = registry.createPlayAt(effect, emitter, x, y, z, yaw, pitch);
                    } else {
                        message = registry.createPlayAt(effect, emitter, x, y, z);
                    }

                    sendAtPos(message, world, x, y, z);
                } else {
                    Entity entity = getEntity(server, sender, args[2]);
                    message = registry.createPlayWith(effect, emitter, entity.getEntityId());

                    sendAtPos(message, entity.world, entity.posX, entity.posY, entity.posZ);
                }
            }
        }
    }

    List<String> completePlay(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {

        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, registry.keySets());
        }
        else if (args.length == 3)
        {
            ArrayList<String> list = new ArrayList<>();

            for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                list.add(player.getName());
            }

            return getListOfStringsMatchingLastWord(args, list);
        } else {
            return Collections.emptyList();
        }
    }

    void executeStop(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length < 3) {
            throw new WrongUsageException("commands.effek.stop.usage");
        }
        else
        {
            SPacketStop message = registry.createStop(args[0], args[1]);

            if (message == null) throw new EffectNotFoundException(args[0]);

            if (args.length > 5) {
                World world = getWorld(args[2]);
                double x = parseDouble(args[3], -30000000, 30000000);
                double y = parseDouble(args[4], -30000000, 30000000);
                double z = parseDouble(args[5], -30000000, 30000000);

                sendAtPos(message, world, x, y, z);
            }
            else
            {
                Entity entity = getEntity(server, sender, args[2]);
                sendAtPos(message, entity.world, entity.posX, entity.posY, entity.posZ);
            }
        }
    }

    List<String> completeStop(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, registry.keySets());
        } else if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, server.getPlayerList().getOnlinePlayerNames());
        } else return Collections.emptyList();
    }

    void executeClear(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1)
            throw new WrongUsageException("commands.effek.clear.usage");
        else {

            EntityPlayer player = getPlayer(server, sender, args[0]);

            if (!check.test(player.getPersistentID())) return;

            wrapper.sendTo(new SPacketClear(), player);

        }
    }

    List<String> completeClear(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getPlayerList().getOnlinePlayerNames());
        }
        else return Collections.emptyList();
    }

    void executeVersion(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Object[] placeholders = new Object[] {version, Constants.PROTOCOL_VERSION, "forge"};
        sender.sendMessage(new TextComponentTranslation("commands.effek.version.display", placeholders));
    }

    private interface LambdaExec {
        void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
    }

    private interface LambdaComp {
        List<String> compile(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos);
    }

    private static class Adaptor extends CommandBase {
        private final String name;
        private final LambdaExec exec;
        private final LambdaComp comp;

        protected Adaptor(String name, LambdaExec exec) {
            this.name = name;
            this.exec = exec;
            this.comp = null;
        }

        protected Adaptor(String name, LambdaExec exec, LambdaComp comp) {
            this.name = name;
            this.exec = exec;
            this.comp = comp;
        }

        @Override
        @Nonnull
        public String getName() {
            return name;
        }

        @Override
        @Nonnull
        public String getUsage(@Nonnull ICommandSender sender) {
            return "commands.effek." + name + ".usage";
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
            exec.execute(server, sender, args);
        }

        @Override
        @Nonnull
        public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
            return comp == null ? super.getTabCompletions(server, sender, args, targetPos) : comp.compile(server, sender, args, targetPos);
        }
    }

    private static class EffectNotFoundException extends CommandException {
        EffectNotFoundException(String effect) {
            super("commands.effek.effect.notFound", effect);
        }
    }
    private static class WorldNotFountException extends CommandException {
        WorldNotFountException(int dim) {
            super("commands.effek.world.notFound,dim", dim);
        }
    }
}
