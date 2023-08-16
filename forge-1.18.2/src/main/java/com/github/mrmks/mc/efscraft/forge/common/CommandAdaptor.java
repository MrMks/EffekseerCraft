package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandAdaptor {

    static final Map<String, PermissionNode<Boolean>> NODES = ImmutableMap.of(
            "efscraft.command", new PermissionNode<>("efscraft", "command", PermissionTypes.BOOLEAN, ((p, u, c) -> p == null || p.server.getPlayerList().isOp(p.getGameProfile()) ? Boolean.TRUE : Boolean.FALSE))
    );

    private static final Splitter SPLITTER = Splitter.on(' ');

    private final EfsServerImpl server;

    CommandAdaptor(EfsServerImpl server) {
        this.server = server;
    }

    static class ExceptionWrapper extends EfsServerCommandHandler.CommandException {

        final CommandSyntaxException exception;

        ExceptionWrapper(CommandSyntaxException e) {
            super("wrapped");
            this.exception = e;
        }
    }

    static void register(CommandAdaptor adaptor, CommandDispatcher<CommandSourceStack> dispatcher) {

        Command<CommandSourceStack> exec = adaptor::execute;
        SuggestionProvider<CommandSourceStack> comp = adaptor::complete;

        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("effek")
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

    Pair<String, String[]> parseInput(String commandLine) {
        Iterator<String> it = SPLITTER.split(commandLine).iterator();

        String label = it.hasNext() ? it.next() : null;
        List<String> list = new ArrayList<>();
        it.forEachRemaining(list::add);

        return new ImmutablePair<>(label, list.toArray(new String[0]));
    }

    int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        try {
            server.executeCommands(pair.getLeft(), pair.getRight(), context, context.getSource().getServer());
        } catch (EfsServerCommandHandler.CommandException e) {
            if (e instanceof ExceptionWrapper)
                throw ((ExceptionWrapper) e).exception;
            else
                throw new CommandRuntimeException(new TranslatableComponent(e.getMessage(), e.getParams()));
        }

        return Command.SINGLE_SUCCESS;
    }

    CompletableFuture<Suggestions> complete(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        Collection<String> results = server.completeCommands(pair.getLeft(), pair.getRight(), context, context.getSource().getServer());

        builder = builder.createOffset(builder.getInput().lastIndexOf(32) + 1);

        for (String s : results) {
            builder.suggest(s);
        }

        return builder.buildFuture();

    }
}