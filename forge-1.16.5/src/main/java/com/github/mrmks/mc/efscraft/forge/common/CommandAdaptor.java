package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.EfsCommandHandler;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import com.google.common.base.Splitter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.RotationArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandAdaptor implements EfsCommandHandler.Adaptor<Entity, PlayerEntity, CommandContext<CommandSource>, CommandSource, World> {

    private static final Splitter SPLITTER = Splitter.on(' ');

    private final NetworkWrapper wrapper;
    private final EfsCommandHandler<Entity, PlayerEntity, CommandContext<CommandSource>, CommandSource, World> handler;

    CommandAdaptor(NetworkWrapper wrapper, File file, Map<UUID, PacketHello.State> clients, String modVersion) {
        this.wrapper = wrapper;
        this.handler = new EfsCommandHandler<>(this, file, "forge", modVersion, clients);
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
    public UUID getClientUUID(PlayerEntity sender) {
        return sender.getUUID();
    }

    @Override
    public void sendPacketTo(CommandContext<CommandSource> server, PlayerEntity entity, NetworkPacket message) {
        wrapper.sendTo(entity, message);
    }

    @Override
    public PlayerEntity findPlayer(CommandContext<CommandSource> server, CommandSource sender, String toFound) throws EfsCommandHandler.CommandException {
        try {
            return EntityArgument.getPlayer(server, "target");
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Entity findEntity(CommandContext<CommandSource> server, CommandSource iCommandSource, String toFound) throws EfsCommandHandler.CommandException {
        try {
            return EntityArgument.getEntity(server, "target");
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
    public World findWorld(CommandContext<CommandSource> server, CommandSource sender, String str) throws EfsCommandHandler.CommandException {
        try {
            return DimensionArgument.getDimension(server, "dim");
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
    public int getViewDistance(World world) {
        MinecraftServer server = world == null ? null : world.getServer();
        PlayerList list = server == null ? null : server.getPlayerList();

        return list == null ? 10 : list.getViewDistance();
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

    private static class ExceptionWrapper extends EfsCommandHandler.CommandException {

        final CommandSyntaxException exception;

        protected ExceptionWrapper(CommandSyntaxException e) {
            super("wrapped");
            this.exception = e;
        }
    }

    void register(CommandDispatcher<CommandSource> dispatcher) {

        Command<CommandSource> exec = this::execute;
        SuggestionProvider<CommandSource> comp = this::complete;

        LiteralArgumentBuilder<CommandSource> builder = Commands.literal("effek")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("play")
                        .then(Commands.argument("effect", StringArgumentType.string()).suggests(comp)
                                .then(Commands.argument("emitter", StringArgumentType.string()).suggests(comp)
                                                .then(Commands.literal("on")
                                                        .then(Commands.argument("target", EntityArgument.entity()).suggests(comp).executes(exec)
                                                                .then(Commands.argument("options", StringArgumentType.greedyString()).executes(exec))
                                                        )
                                                )
                                                .then(Commands.literal("at")
                                                        .then(Commands.argument("dim", DimensionArgument.dimension()).suggests(comp)
                                                                .then(Commands.argument("location", Vec3Argument.vec3()).suggests(comp)
                                                                        .then(Commands.argument("rotation", RotationArgument.rotation()).suggests(comp).executes(exec)
                                                                                .then(Commands.argument("options", StringArgumentType.greedyString()).executes(exec))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                        )
                )
                .then(Commands.literal("stop")
                        .then(Commands.argument("effect", StringArgumentType.string()).suggests(comp)
                                .then(Commands.argument("emitter", StringArgumentType.string()).suggests(comp)
                                        .then(Commands.literal("on")
                                                .then(Commands.argument("target", EntityArgument.entity()).suggests(comp).executes(exec))
                                        )
                                        .then(Commands.literal("at")
                                                .then(Commands.argument("dim", DimensionArgument.dimension()).suggests(comp)
                                                        .then(Commands.argument("location", Vec3Argument.vec3()).suggests(comp).executes(exec))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player()).suggests(comp).executes(exec))
                )
                .then(Commands.literal("reload").executes(exec))
                .then(Commands.literal("version").executes(exec));

        dispatcher.register(builder);
    }

    private ArgumentBuilder<CommandSource, ?> builderEffect(ArgumentBuilder<CommandSource, ?> builder) {

        SuggestionProvider<CommandSource> comp = this::complete;

        ArgumentBuilder<CommandSource, ?> last;

        builder.then(Commands.argument("effect", StringArgumentType.string()).suggests(comp)
                .then(last = Commands.argument("emitter", StringArgumentType.string())).suggests(comp)
        );

        return last;
    }

    Pair<String, String[]> parseInput(String commandLine) {
        Iterator<String> it = SPLITTER.split(commandLine).iterator();

        String label = it.hasNext() ? it.next() : null;
        List<String> list = new ArrayList<>();
        it.forEachRemaining(list::add);

        return new ImmutablePair<>(label, list.toArray(new String[0]));
    }

    int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        try {
            handler.dispatchExecute(pair.getLeft(), pair.getRight(), context, context.getSource());
        } catch (EfsCommandHandler.CommandException e) {
            if (e instanceof ExceptionWrapper)
                throw ((ExceptionWrapper) e).exception;
            else
                throw new CommandException(new TranslationTextComponent(e.getMessage(), e.getParams()));
        }

        return Command.SINGLE_SUCCESS;
    }

    CompletableFuture<Suggestions> complete(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        Collection<String> results = handler.dispatchComplete(pair.getLeft(), pair.getRight(), context, context.getSource());

        builder = builder.createOffset(builder.getInput().lastIndexOf(32) + 1);

        for (String s : results) {
            builder.suggest(s);
        }

        return builder.buildFuture();

    }
}