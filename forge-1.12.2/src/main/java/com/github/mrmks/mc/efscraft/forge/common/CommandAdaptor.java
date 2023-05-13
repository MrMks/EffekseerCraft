package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.CommandHandler;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CommandAdaptor extends CommandBase implements CommandHandler.Adaptor<Entity, EntityPlayer, MinecraftServer, ICommandSender, World> {

    private final NetworkWrapper wrapper;
    private final CommandHandler<Entity, EntityPlayer, MinecraftServer, ICommandSender, World> handler;
    private final Set<UUID> compatibleClients;

    @Override
    public boolean hasPermission(MinecraftServer server, ICommandSender sender, String node) {
        if (sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, node);
        } else {
            return true;
        }
    }

    @Override
    public boolean isClientValid(EntityPlayer sender) {
        return compatibleClients.contains(sender.getPersistentID());
    }

    @Override
    public void sendPacketTo(MinecraftServer server, EntityPlayer entityPlayer, IMessage message) {
        wrapper.sendTo(entityPlayer, message);
    }

    @Override
    public EntityPlayer findPlayer(MinecraftServer server, ICommandSender sender, String toFound) throws CommandHandler.CommandException {
        try {
            return getPlayer(server, sender, toFound);
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public Entity findEntity(MinecraftServer server, ICommandSender sender, String toFound) throws CommandHandler.CommandException {
        try {
            return getEntity(server, sender, toFound);
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public Collection<EntityPlayer> getPlayersInWorld(MinecraftServer server, ICommandSender sender, World world) {
        return new ArrayList<>(world.playerEntities);
    }

    @Override
    public Collection<String> completePlayers(MinecraftServer server) {
        return Arrays.asList(server.getPlayerList().getOnlinePlayerNames());
    }

    @Override
    public Collection<String> completeWorlds(MinecraftServer server) {
        return Arrays.stream(DimensionManager.getIDs()).map(Object::toString).collect(Collectors.toList());
    }

    @Override
    public void sendMessage(ICommandSender sender, String msg, Object[] objects, boolean schedule) {
        if (schedule) {
            MinecraftServer server = sender.getServer();
            if (server != null)
                server.addScheduledTask(() -> sendMessage(sender, msg, objects,false));
        } else {
            Entity entity = sender.getCommandSenderEntity();
            if (entity == null) {
                sender.sendMessage(new TextComponentTranslation(msg, objects));
            } else if (entity.isAddedToWorld()) {
                if (entity instanceof EntityPlayer || entity.isEntityAlive())
                    sender.sendMessage(new TextComponentTranslation(msg, objects));
            }
        }
    }

    @Override
    public float[] getEntityPosAngle(Entity entity) {
        return new float[]{(float) entity.posX, (float) entity.posY, (float) entity.posZ, entity.rotationYaw, entity.rotationPitch};
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getEntityId();
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.getEntityWorld();
    }

    @Override
    public World findWorld(MinecraftServer server, ICommandSender sender, String str) throws CommandHandler.CommandException {
        try {
            return DimensionManager.getWorld(parseInt(str));
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public float[] getSenderPosAngle(ICommandSender sender) {
        return sender instanceof Entity ? getEntityPosAngle((Entity) sender) : new float[5];
    }

    private static class CommandExceptionWrapper extends CommandHandler.CommandException {
        CommandException ce;
        protected CommandExceptionWrapper(CommandException e) {
            super(e.getMessage(), e.getErrorObjects());
            ce = e;
        }
    }

    // ====== ======

    CommandAdaptor(NetworkWrapper wrapper, File file, Set<UUID> clients, String modVersion) {
        this.wrapper = wrapper;
        this.handler = new CommandHandler<>(this, file, "forge", modVersion);
        this.compatibleClients = clients;
    }

    @Override @Nonnull
    public String getName() {
        return "effek";
    }

    @Override @Nonnull
    public String getUsage(@Nullable ICommandSender sender) {
        return "commands.effek.usage";
    }

    @Override
    public void execute(@Nullable MinecraftServer server, @Nullable ICommandSender sender, @Nullable String[] args) throws CommandException {
        try {
            handler.dispatchExecute("effek", args, server, sender);
        } catch (CommandHandler.CommandException e) {
            if (e instanceof CommandExceptionWrapper)
                throw ((CommandExceptionWrapper) e).ce;
            if (e instanceof CommandHandler.WrongUsageException)
                throw new WrongUsageException(e.getMessage(), e.getParams());
            else
                throw new CommandException(e.getMessage(), e.getParams());
        }
    }

    @Override @Nonnull
    public List<String> getTabCompletions(@Nullable MinecraftServer server, @Nullable ICommandSender sender, @Nullable String[] args, @Nullable BlockPos targetPos) {
        return handler.dispatchComplete("effek", args, server, sender);
    }
}
