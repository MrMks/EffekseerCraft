package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.CommandHandler;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
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
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandAdaptor implements CommandHandler.Adaptor<Entity, Player, CommandContext<CommandSourceStack>, CommandSourceStack, Level> {

    static final Map<String, PermissionNode<Boolean>> NODES = ImmutableMap.of(
            "efscraft.commands", new PermissionNode<>("efscraft", "commands", PermissionTypes.BOOLEAN, ((p, u, c) -> p == null || p.server.getPlayerList().isOp(p.getGameProfile()) ? Boolean.TRUE : Boolean.FALSE))
    );

    private static final Splitter SPLITTER = Splitter.on(' ');

    private final NetworkWrapper wrapper;
    private final CommandHandler<Entity, Player, CommandContext<CommandSourceStack>, CommandSourceStack, Level> handler;

    CommandAdaptor(NetworkWrapper wrapper, File file, Map<UUID, PacketHello.State> clients, String modVersion) {
        this.wrapper = wrapper;
        this.handler = new CommandHandler<>(this, file, "forge", modVersion, clients);
    }

    @Override
    public boolean hasPermission(CommandContext<CommandSourceStack> server, CommandSourceStack sender, String node) {
        
        PermissionNode<Boolean> permNode = NODES.get(node);
        
        if (permNode == null) return false;

        Entity entity = sender.getEntity();
        
        if (entity instanceof ServerPlayer)
            return PermissionAPI.getPermission((ServerPlayer) entity, permNode);
        else
            return true;
    }

    @Override
    public UUID getClientUUID(Player sender) {
        return sender.getUUID();
    }

    @Override
    public void sendPacketTo(CommandContext<CommandSourceStack> server, Player entity, NetworkPacket message) {
        wrapper.sendTo(entity, message);
    }

    @Override
    public Player findPlayer(CommandContext<CommandSourceStack> server, CommandSourceStack sender, String toFound) throws CommandHandler.CommandException {
        try {
            return EntityArgument.getPlayer(server, "target");
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Entity findEntity(CommandContext<CommandSourceStack> server, CommandSourceStack iCommandSourceStack, String toFound) throws CommandHandler.CommandException {
        try {
            return EntityArgument.getEntity(server, "target");
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Collection<Player> getPlayersInWorld(CommandContext<CommandSourceStack> server, CommandSourceStack sender, Level world) {
        return new ArrayList<>(world.players());
    }

    @Override
    public float[] getSenderPosAngle(CommandSourceStack sender) {
        Entity entity = sender.getEntity();
        return getEntityPosAngle(entity);
    }

    @Override
    public int getEntityId(Entity entity) {
        return entity.getId();
    }

    @Override
    public float[] getEntityPosAngle(Entity entity) {
        return entity == null ? new float[5] : new float[] {(float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), entity.getYRot(), entity.getXRot()};
    }

    @Override
    public Level getEntityWorld(Entity entity) {
        return entity.level;
    }

    @Override
    public Level findWorld(CommandContext<CommandSourceStack> server, CommandSourceStack sender, String str) throws CommandHandler.CommandException {
        try {
            return DimensionArgument.getDimension(server, "dim");
        } catch (CommandSyntaxException e) {
            throw new ExceptionWrapper(e);
        }
    }

    @Override
    public Collection<String> completePlayers(CommandContext<CommandSourceStack> server) {
        return server.getSource().getOnlinePlayerNames();
    }

    @Override
    public Collection<String> completeWorlds(CommandContext<CommandSourceStack> server) {
        return server.getSource().getServer().levelKeys().stream().map(it -> it.location().toString()).collect(Collectors.toSet());
    }

    @Override
    public int getViewDistance(Level world) {
        MinecraftServer server = world == null ? null : world.getServer();
        PlayerList list = server == null ? null : server.getPlayerList();

        return list == null ? 10 : list.getViewDistance();
    }

    @Override
    public void sendMessage(CommandSourceStack sender, String msg, Object[] objects, boolean schedule) {
        if (schedule)
            sender.getServer().submit(() -> sendMessage0(sender, msg, objects));
        else
            sendMessage0(sender, msg, objects);
    }

    private void sendMessage0(CommandSourceStack source, String msg, Object[] objects) {
        source.sendSuccess(new TranslatableComponent(msg, objects), false);
    }

    private static class ExceptionWrapper extends CommandHandler.CommandException {

        final CommandSyntaxException exception;

        protected ExceptionWrapper(CommandSyntaxException e) {
            super("wrapped");
            this.exception = e;
        }
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        Command<CommandSourceStack> exec = this::execute;
        SuggestionProvider<CommandSourceStack> comp = this::complete;

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

    private ArgumentBuilder<CommandSourceStack, ?> builderEffect(ArgumentBuilder<CommandSourceStack, ?> builder) {

        SuggestionProvider<CommandSourceStack> comp = this::complete;

        ArgumentBuilder<CommandSourceStack, ?> last;

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

    int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        try {
            handler.dispatchExecute(pair.getLeft(), pair.getRight(), context, context.getSource());
        } catch (CommandHandler.CommandException e) {
            if (e instanceof ExceptionWrapper)
                throw ((ExceptionWrapper) e).exception;
            else
                throw new CommandRuntimeException(new TranslatableComponent(e.getMessage(), e.getParams()));
        }

        return Command.SINGLE_SUCCESS;
    }

    CompletableFuture<Suggestions> complete(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Pair<String, String[]> pair = parseInput(context.getInput().substring(1));
        Collection<String> results = handler.dispatchComplete(pair.getLeft(), pair.getRight(), context, context.getSource());

        builder = builder.createOffset(builder.getInput().lastIndexOf(32) + 1);

        for (String s : results) {
            builder.suggest(s);
        }

        return builder.buildFuture();

    }
}