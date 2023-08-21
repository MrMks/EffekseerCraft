package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.github.mrmks.mc.efscraft.server.IEfsServerAdaptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

class EfsServerAdaptorImpl implements IEfsServerAdaptor<MinecraftServer, WorldServer, Entity, EntityPlayerMP, ICommandSender, ByteBufOutputStream> {

    private static class CommandExceptionWrapper extends EfsServerCommandHandler.CommandException {

        public CommandExceptionWrapper(CommandException exception) {
            super(exception.getMessage(), exception.getErrorObjects());
        }
    }

    NetworkWrapper wrapper;

    EfsServerAdaptorImpl(NetworkWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public boolean hasPermission(MinecraftServer minecraftServer, ICommandSender sender, String permissionNode) {
        Entity entity = sender.getCommandSenderEntity();
        if (entity instanceof EntityPlayer)
            return PermissionAPI.hasPermission((EntityPlayer) entity, permissionNode);
        else
            return true;
    }

    @Override
    public EntityPlayerMP getPlayer(MinecraftServer server, UUID uuid) {
        return server == null ? null : server.getPlayerList().getPlayerByUUID(uuid);
    }

    @Override
    public Entity findEntity(MinecraftServer minecraftServer, ICommandSender sender, String toFind) throws EfsServerCommandHandler.CommandException {
        try {
            return CommandBase.getEntity(minecraftServer, sender, toFind);
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public EntityPlayerMP findPlayer(MinecraftServer minecraftServer, ICommandSender sender, String toFind) throws EfsServerCommandHandler.CommandException {
        try {
            return CommandBase.getPlayer(minecraftServer, sender, toFind);
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public Entity getEntity(MinecraftServer minecraftServer, WorldServer world, UUID uuid) {
        return world.getEntityFromUuid(uuid);
    }

    @Override
    public EntityPlayerMP getPlayerEntity(Entity entity) {
        return entity instanceof EntityPlayerMP ? (EntityPlayerMP) entity : null;
    }

    @Override
    public Entity getEntitySender(ICommandSender sender) {
        if (sender instanceof Entity)
            return (Entity) sender;
        else
            return null;
    }

    @Override
    public WorldServer getWorld(MinecraftServer minecraftServer, String world) throws EfsServerCommandHandler.CommandException {
        try {
            return minecraftServer.getWorld(CommandBase.parseInt(world));
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public WorldServer getWorld(MinecraftServer minecraftServer, ICommandSender sender, String world) throws EfsServerCommandHandler.CommandException {
        try {
            return minecraftServer.getWorld(CommandBase.parseInt(world));
        } catch (CommandException e) {
            throw new CommandExceptionWrapper(e);
        }
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getEntityId();
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getPersistentID();
    }

    @Override
    public WorldServer getEntityWorld(Entity entity) {
        World world = entity.world;

        return (WorldServer) world;
    }

    @Override
    public Vec3f getEntityPos(Entity entity) {
        return new Vec3f(entity.posX, entity.posY, entity.posZ);
    }

    @Override
    public Vec2f getEntityAngle(Entity entity) {
        return new Vec2f(entity.rotationYaw, entity.rotationPitch);
    }

    @Override
    public String getPlayerName(EntityPlayerMP player) {
        return player.getName();
    }

    @Override
    public String getWorldName(WorldServer world) {
        return Integer.toString(world.provider.getDimension());
    }

    @Override
    public int getWorldViewDistance(WorldServer world) {
        MinecraftServer server = world.getMinecraftServer();

        return server == null ? 10 : server.getPlayerList().getViewDistance();
    }

    @Override
    public Vec3f getSenderPos(ICommandSender sender) {
        Vec3d vec3d = sender.getPositionVector();
        return new Vec3f(vec3d.x, vec3d.y, vec3d.z);
    }

    @Override
    public List<WorldServer> getWorlds(MinecraftServer minecraftServer) {
        return Arrays.asList(minecraftServer.worlds);
    }

    @Override
    public List<WorldServer> getWorlds(MinecraftServer minecraftServer, ICommandSender sender) {
        return Arrays.asList(minecraftServer.worlds);
    }

    @Override
    public List<EntityPlayerMP> getPlayersInWorld(WorldServer world) {
        return world.getPlayers(EntityPlayerMP.class, any -> true);
    }

    @Override
    public List<EntityPlayerMP> getPlayersInServer(MinecraftServer minecraftServer) {
        return minecraftServer.getPlayerList().getPlayers();
    }

    @Override
    public List<EntityPlayerMP> getPlayersInServer(MinecraftServer minecraftServer, ICommandSender sender) {
        return minecraftServer.getPlayerList().getPlayers();
    }

    @Override
    public ByteBufOutputStream createOutput() {
        return new ByteBufOutputStream(Unpooled.buffer());
    }

    @Override
    public void closeOutput(ByteBufOutputStream output) {
        ByteBuf buf = output.buffer();
        if (buf.refCnt() > 0)
            buf.release();
    }

    @Override
    public void sendPacket(Collection<EntityPlayerMP> players, Predicate<EntityPlayerMP> test, ByteBufOutputStream output) {
        ByteBuf buf = output.buffer();
        players.stream().filter(test).forEach(pl -> wrapper.sendTo(pl, buf));
    }

    @Override
    public void sendPacket(MinecraftServer minecraftServer, Collection<EntityPlayerMP> players, Predicate<EntityPlayerMP> test, ByteBufOutputStream output) {
        sendPacket(players, test, output);
    }

    @Override
    public void sendMessage(MinecraftServer minecraftServer, ICommandSender sender, String msg, Object[] args, boolean scheduled) {
        if (scheduled) {
            minecraftServer.addScheduledTask(() -> sendMessage(minecraftServer, sender, msg, args, false));
        } else {
            sender.sendMessage(new TextComponentTranslation(msg, args));
        }
    }
}
