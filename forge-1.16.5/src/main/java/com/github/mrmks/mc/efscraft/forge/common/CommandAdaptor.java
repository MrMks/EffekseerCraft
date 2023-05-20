package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.CommandHandler;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.google.common.base.Splitter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandAdaptor implements CommandHandler.Adaptor<Entity, PlayerEntity, CommandContext<CommandSource>, CommandSource, World> {

    private static final Splitter SPLITTER = Splitter.on(' ');

    private final NetworkWrapper wrapper;
    private final Set<UUID> compatibleClients;
    private final CommandHandler<Entity, PlayerEntity, CommandContext<CommandSource>, CommandSource, World> handler;

    CommandAdaptor(NetworkWrapper wrapper, File file, Set<UUID> clients, String modVersion) {
        this.wrapper = wrapper;
        this.handler = new CommandHandler<>(this, file, "forge", modVersion);
        this.compatibleClients = clients;
    }

    @Override
    public boolean hasPermission(CommandContext<CommandSource> server, CommandSource sender, String node) {
        Entity entity = sender.getEntity();
        if (entity instanceof PlayerEntity)
            return PermissionAPI.hasPermission((PlayerEntity) entity, node);
        else
            return true;
    }

    @Override
    public boolean isClientValid(PlayerEntity sender) {
        return compatibleClients.contains(sender.getUUID());
    }

    @Override
    public void sendPacketTo(CommandContext<CommandSource> server, PlayerEntity entity, IMessage message) {
        wrapper.sendTo(entity, message);
    }

    @Override
    public PlayerEntity findPlayer(CommandContext<CommandSource> server, CommandSource sender, String toFound) throws CommandHandler.CommandException {
        try {
            return EntityArgument.getPlayer(server, toFound);
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Entity findEntity(CommandContext<CommandSource> server, CommandSource iCommandSource, String toFound) throws CommandHandler.CommandException {
        try {
            return EntityArgument.getEntity(server, toFound);
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Collection<PlayerEntity> getPlayersInWorld(CommandContext<CommandSource> server, CommandSource sender, World world) {
        return new ArrayList<>(world.players());
    }

    @Override
    public float[] getSenderPosAngle(CommandSource sender) {
        Entity entity = sender.getEntity();
        return getEntityPosAngle(entity);
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getId();
    }

    @Override
    public float[] getEntityPosAngle(Entity entity) {
        return entity == null ? new float[5] : new float[] {(float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), entity.yRot, entity.xRot};
    }

    @Override
    public World getEntityWorld(Entity entity) {
        return entity.level;
    }

    @Override
    public World findWorld(CommandContext<CommandSource> server, CommandSource sender, String str) throws CommandHandler.CommandException {
        try {
            return DimensionArgument.getDimension(server, str);
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Collection<String> completePlayers(CommandContext<CommandSource> server) {
        return server.getSource().getOnlinePlayerNames();
    }

    @Override
    public Collection<String> completeWorlds(CommandContext<CommandSource> server) {
        return server.getSource().getServer().levelKeys().stream().map(it -> it.location().toString()).collect(Collectors.toSet());
    }

    @Override
    public void sendMessage(CommandSource sender, String msg, Object[] objects, boolean schedule) {
        if (schedule)
            sender.getServer().submit(() -> sendMessage0(sender, msg, objects));
        else
            sendMessage0(sender, msg, objects);
    }

    private void sendMessage0(CommandSource source, String msg, Object[] objects) {
        source.sendSuccess(new TranslationTextComponent(msg, objects), false);
    }

    private static class ExceptionWrapper extends CommandHandler.CommandException {

        final CommandSyntaxException exception;

        protected ExceptionWrapper(CommandSyntaxException e) {
            super("wrapped");
            this.exception = e;
        }
    }

    void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> builder = Commands.literal("effek")
                .requires(source -> source.hasPermission(3))
                .executes(this::execute)
                .then(Commands.argument("args", StringArgumentType.greedyString()).suggests(this::complete).executes(this::execute));
        dispatcher.register(builder);
    }

    Pair<String, String[]> parseInput(String commandLine) {
        Iterator<String> it = Splitter.on(' ').split(commandLine).iterator();

        String label = it.hasNext() ? it.next() : null;
        List<String> list = new ArrayList<>();
        it.forEachRemaining(list::add);

        return new ImmutablePair<>(label, list.toArray(new String[0]));
    }

    int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput());
        try {
            handler.dispatchExecute(pair.getLeft(), pair.getRight(), context, context.getSource());
        } catch (CommandHandler.CommandException e) {
            if (e instanceof ExceptionWrapper)
                throw ((ExceptionWrapper) e).exception;
            else
                throw new CommandException(new TranslationTextComponent(e.getMessage(), e.getParams()));
        }

        return Command.SINGLE_SUCCESS;
    }

    CompletableFuture<Suggestions> complete(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput());
        Collection<String> results = handler.dispatchComplete(pair.getLeft(), pair.getRight(), context, context.getSource());

        builder = builder.createOffset(builder.getInput().lastIndexOf(32) + 1);

        for (String s : results) {
            builder.suggest(s);
        }

        return builder.buildFuture();

    }
}