package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.github.mrmks.mc.efscraft.server.EfsServerEnv;
import com.github.mrmks.mc.efscraft.server.IEfsServerAdaptor;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

class EfsServerImpl extends EfsServer<MinecraftServer, ServerLevel, Entity, ServerPlayer, CommandContext<CommandSourceStack>, ByteBufInputStream, ByteBufOutputStream> {
    EfsServerImpl(NetworkWrapper wrapper, LogAdaptor logger, String implVer) {
        super(new AdaptorImpl(wrapper), logger, EfsServerEnv.FORGE, implVer, false);

        wrapper.setServer(this);
    }

    private static class AdaptorImpl implements IEfsServerAdaptor<MinecraftServer, ServerLevel, Entity, ServerPlayer, CommandContext<CommandSourceStack>, ByteBufInputStream, ByteBufOutputStream> {

        private final NetworkWrapper wrapper;
        private AdaptorImpl(NetworkWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public boolean hasPermission(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender, String permissionNode) {
            PermissionNode<Boolean> permNode = CommandAdaptor.NODES.get(permissionNode);

            if (permNode == null) return false;

            Entity entity = sender.getSource().getEntity();

            if (entity instanceof ServerPlayer)
                return PermissionAPI.getPermission((ServerPlayer) entity, permNode);
            else
                return true;
        }

        @Override
        public ServerPlayer getPlayer(MinecraftServer minecraftServer, UUID uuid) {
            return minecraftServer.getPlayerList().getPlayer(uuid);
        }

        @Override
        public Entity findEntity(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender, String toFind) throws EfsServerCommandHandler.CommandException {
            try {
                return EntityArgument.getEntity(sender, "target");
            } catch (CommandSyntaxException e) {
                throw new CommandAdaptor.ExceptionWrapper(e);
            }
        }

        @Override
        public ServerPlayer findPlayer(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender, String toFind) throws EfsServerCommandHandler.CommandException {
            try {
                return EntityArgument.getPlayer(sender, "target");
            } catch (CommandSyntaxException e) {
                throw new CommandAdaptor.ExceptionWrapper(e);
            }
        }

        @Override
        public Entity getEntity(MinecraftServer minecraftServer, ServerLevel world, UUID uuid) {
            return world.getEntity(uuid);
        }

        @Override
        public ServerPlayer getPlayerEntity(Entity entity) {
            return entity instanceof ServerPlayer ? (ServerPlayer) entity : null;
        }

        @Override
        public Entity getEntitySender(CommandContext<CommandSourceStack> sender) {
            return sender.getSource().getEntity();
        }

        @Override
        public ServerLevel getWorld(MinecraftServer minecraftServer, String world) throws EfsServerCommandHandler.CommandException {
            return minecraftServer.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(world)));
        }

        @Override
        public ServerLevel getWorld(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender, String world) throws EfsServerCommandHandler.CommandException {
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
        public ServerLevel getEntityWorld(Entity entity) {
            return (ServerLevel) entity.level;
        }

        @Override
        public Vec3f getEntityPos(Entity entity) {
            return new Vec3f(entity.getX(), entity.getY(), entity.getZ());
        }

        @Override
        public Vec2f getEntityAngle(Entity entity) {
            return new Vec2f(entity.getYRot(), entity.getXRot());
        }

        @Override
        public String getPlayerName(ServerPlayer player) {
            return player.getName().getContents();
        }

        @Override
        public String getWorldName(ServerLevel world) {
            return world.dimension().location().toString();
        }

        @Override
        public int getWorldViewDistance(ServerLevel world) {
            return world.getServer().getPlayerList().getViewDistance();
        }

        @Override
        public Vec3f getSenderPos(CommandContext<CommandSourceStack> sender) {
            Vec3 vec3 = sender.getSource().getPosition();
            return new Vec3f(vec3.x, vec3.y, vec3.z);
        }

        @Override
        public List<ServerLevel> getWorlds(MinecraftServer minecraftServer) {
            ArrayList<ServerLevel> list = new ArrayList<>();
            for (ServerLevel level : minecraftServer.getAllLevels())
                list.add(level);

            return list;
        }

        @Override
        public List<ServerLevel> getWorlds(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender) {
            return getWorlds(minecraftServer);
        }

        @Override
        public List<ServerPlayer> getPlayersInWorld(ServerLevel world) {
            return new ArrayList<>(world.players());
        }

        @Override
        public List<ServerPlayer> getPlayersInServer(MinecraftServer minecraftServer) {
            return new ArrayList<>(minecraftServer.getPlayerList().getPlayers());
        }

        @Override
        public List<ServerPlayer> getPlayersInServer(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender) {
            return getPlayersInServer(minecraftServer);
        }

        @Override
        public ByteBufOutputStream createOutput() {
            return new ByteBufOutputStream(Unpooled.buffer());
        }

        @Override
        public void closeOutput(ByteBufOutputStream output) throws IOException {
            ByteBuf buf = output.buffer();
            buf.release();
        }

        @Override
        public void sendPacket(Collection<ServerPlayer> players, Predicate<ServerPlayer> test, ByteBufOutputStream output) {

            ByteBuf buf = output.buffer();

            players.stream()
                    .filter(test)
                    .forEach(it -> wrapper.sendTo(it, buf));
        }

        @Override
        public void sendPacket(MinecraftServer minecraftServer, Collection<ServerPlayer> players, Predicate<ServerPlayer> test, ByteBufOutputStream output) {
            sendPacket(players, test, output);
        }

        @Override
        public void sendMessage(MinecraftServer minecraftServer, CommandContext<CommandSourceStack> sender, String msg, Object[] args, boolean scheduled) {
            if (scheduled) {
                minecraftServer.submit(() -> sendMessage(minecraftServer, sender, msg, args, false));
            } else {
                sender.getSource().sendSuccess(new TranslatableComponent(msg, args), true);
            }
        }
    }
}
