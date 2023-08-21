package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.github.mrmks.mc.efscraft.server.IEfsServerAdaptor;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.server.permission.PermissionAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class EfsServerAdaptorImpl implements IEfsServerAdaptor<MinecraftServer, ServerWorld, Entity, ServerPlayerEntity,
        CommandContext<CommandSource>, ByteBufOutputStream> {

    private final NetworkWrapper wrapper;
    EfsServerAdaptorImpl(NetworkWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public boolean hasPermission(MinecraftServer minecraftServer, CommandContext<CommandSource> sender, String permissionNode) {
        Entity entity = sender.getSource().getEntity();
        if (entity instanceof PlayerEntity)
            return PermissionAPI.hasPermission((PlayerEntity) entity, permissionNode);
        else
            return true;
    }

    @Override
    public ServerPlayerEntity getPlayer(MinecraftServer minecraftServer, UUID uuid) {
        return minecraftServer.getPlayerList().getPlayer(uuid);
    }

    @Override
    public Entity findEntity(MinecraftServer minecraftServer, CommandContext<CommandSource> sender, String toFind) throws EfsServerCommandHandler.CommandException {
        try {
            return EntityArgument.getEntity(sender, "target");
        } catch (CommandSyntaxException e) {
            throw new CommandAdaptor.ExceptionWrapper(e);
        }
    }

    @Override
    public ServerPlayerEntity findPlayer(MinecraftServer minecraftServer, CommandContext<CommandSource> sender, String toFind) throws EfsServerCommandHandler.CommandException {
        try {
            return EntityArgument.getPlayer(sender, "target");
        } catch (CommandSyntaxException e) {
            throw new CommandAdaptor.ExceptionWrapper(e);
        }
    }

    @Override
    public Entity getEntity(MinecraftServer minecraftServer, ServerWorld world, UUID uuid) {
        return world.getEntity(uuid);
    }

    @Override
    public ServerPlayerEntity getPlayerEntity(Entity entity) {
        return entity instanceof ServerPlayerEntity ? (ServerPlayerEntity) entity : null;
    }

    @Override
    public Entity getEntitySender(CommandContext<CommandSource> sender) {
        return sender.getSource().getEntity();
    }

    @Override
    public ServerWorld getWorld(MinecraftServer minecraftServer, String world) throws EfsServerCommandHandler.CommandException {
        return minecraftServer.getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(world)));
    }

    @Override
    public ServerWorld getWorld(MinecraftServer minecraftServer, CommandContext<CommandSource> sender, String world) throws EfsServerCommandHandler.CommandException {
        try {
            return DimensionArgument.getDimension(sender, "dim");
        } catch (CommandSyntaxException e) {
            throw new CommandAdaptor.ExceptionWrapper(e);
        }
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getId();
    }

    @Override
    public UUID getEntityUUID(Entity entity) {
        return entity.getUUID();
    }

    @Override
    public ServerWorld getEntityWorld(Entity entity) {
        return (ServerWorld) entity.level;
    }

    @Override
    public Vec3f getEntityPos(Entity entity) {
        return new Vec3f(entity.getX(), entity.getY(), entity.getZ());
    }

    @Override
    public Vec2f getEntityAngle(Entity entity) {
        return new Vec2f(entity.yRot, entity.xRot);
    }

    @Override
    public String getPlayerName(ServerPlayerEntity player) {
        return player.getName().getContents();
    }

    @Override
    public String getWorldName(ServerWorld world) {
        return world.dimension().location().toString();
    }

    @Override
    public int getWorldViewDistance(ServerWorld world) {
        return world.getServer().getPlayerList().getViewDistance();
    }

    @Override
    public Vec3f getSenderPos(CommandContext<CommandSource> sender) {
        Vector3d vector3d = sender.getSource().getPosition();
        return new Vec3f(vector3d.x, vector3d.y, vector3d.z);
    }

    @Override
    public List<ServerWorld> getWorlds(MinecraftServer minecraftServer) {
        List<ServerWorld> worlds = new ArrayList<>();
        for (ServerWorld world : minecraftServer.getAllLevels())
            worlds.add(world);

        return worlds;
    }

    @Override
    public List<ServerWorld> getWorlds(MinecraftServer minecraftServer, CommandContext<CommandSource> sender) {
        return getWorlds(minecraftServer);
    }

    @Override
    public List<ServerPlayerEntity> getPlayersInWorld(ServerWorld world) {
        return new ArrayList<>(world.players());
    }

    @Override
    public List<ServerPlayerEntity> getPlayersInServer(MinecraftServer minecraftServer) {
        return new ArrayList<>(minecraftServer.getPlayerList().getPlayers());
    }

    @Override
    public List<ServerPlayerEntity> getPlayersInServer(MinecraftServer minecraftServer, CommandContext<CommandSource> sender) {
        return getPlayersInServer(minecraftServer);
    }

    @Override
    public ByteBufOutputStream createOutput() {
        return new ByteBufOutputStream(Unpooled.buffer());
    }

    @Override
    public void closeOutput(ByteBufOutputStream output) throws IOException {
        ByteBuf buf = output.buffer();
        if (buf.refCnt() > 0)
            buf.release();
    }

    @Override
    public void sendPacket(Collection<ServerPlayerEntity> players, Predicate<ServerPlayerEntity> test, ByteBufOutputStream output) {
        ByteBuf buf = output.buffer();
        players.stream()
                .filter(test)
                .forEach(it -> wrapper.sendTo(it, buf));
    }

    @Override
    public void sendPacket(MinecraftServer minecraftServer, Collection<ServerPlayerEntity> players, Predicate<ServerPlayerEntity> test, ByteBufOutputStream output) {
        sendPacket(players, test, output);
    }

    @Override
    public void sendMessage(MinecraftServer minecraftServer, CommandContext<CommandSource> sender, String msg, Object[] args, boolean scheduled) {
        if (scheduled) {
            minecraftServer.execute(() -> sendMessage(minecraftServer, sender, msg, args, false));
        } else {
            sender.getSource().sendSuccess(new TranslationTextComponent(msg, args), true);
        }
    }
}
