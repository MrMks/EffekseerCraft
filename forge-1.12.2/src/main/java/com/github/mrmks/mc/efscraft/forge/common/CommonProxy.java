package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import com.github.mrmks.mc.efscraft.server.EfsServerCommandHandler;
import com.github.mrmks.mc.efscraft.server.EfsServerEnv;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.server.event.EfsServerEvent;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommonProxy {
    protected NetworkWrapper wrapper;
    private File configurationFolder;
//    protected Logger logger;
    protected LogAdaptor logAdaptor;
    protected File configFile;

    private EfsServerImpl efsServer;

    public void preInitialize(FMLPreInitializationEvent event) {
        logAdaptor = LogAdaptor.of(event.getModLog());

        configurationFolder = event.getModConfigurationDirectory();
        configFile = event.getSuggestedConfigurationFile();

        wrapper = new NetworkWrapper();
        EfsServerAdaptorImpl serverAdaptor = new EfsServerAdaptorImpl(wrapper);


        efsServer = new EfsServerImpl(
                serverAdaptor,
                logAdaptor,
                EfsServerEnv.FORGE,
                event.getModMetadata().version,
                false);
        wrapper.setServer(efsServer);

        MinecraftForge.EVENT_BUS.register(new EventHandler(efsServer));
    }

    public void initialize(FMLInitializationEvent event) {
        // do nothing
        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");
    }

    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        List<File> files = new ArrayList<>();

        // on server, read registries in ./configs/efscraft/effects.json
        File confDir = new File(configurationFolder, "efscraft");
        File file = new File(confDir, "effects.json");
        files.add(file);
        if (!server.isDedicatedServer()) {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json, too
            file = new File(server.getActiveAnvilConverter().getFile(server.getFolderName(), "efscraft"), "effects.json");
            files.add(file);
        }

        file = new File(confDir, "decrypts.json");
        efsServer.receiveEvent(new EfsServerEvent.Start<>(server, files, file));

        event.registerServerCommand(new CommandHandler(efsServer));
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        efsServer.receiveEvent(EfsServerEvent.Stop.INSTANCE);
    }

    protected static class EfsServerImpl extends EfsServer<MinecraftServer, WorldServer,
                Entity, EntityPlayerMP, ICommandSender, ByteBufOutputStream> {

        public EfsServerImpl(EfsServerAdaptorImpl adaptor, LogAdaptor logger, EfsServerEnv env, String implVer, boolean autoReply) {
            super(adaptor, logger, env, implVer, autoReply);
        }
    }

    protected static class CommandHandler extends CommandBase {

        private final EfsServerImpl server;
        CommandHandler(EfsServerImpl server) {
            this.server = server;
        }

        @Override
        @Nonnull
        public String getName() {
            return "effek";
        }

        @Override
        @Nonnull
        public String getUsage(@Nonnull ICommandSender sender) {
            return "/effek play|stop|trigger|clear [subs...]";
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
            try {
                this.server.executeCommands("effek", args, sender, server);
            } catch (EfsServerCommandHandler.CommandException e) {
                throw new CommandException(e.getMessage(), e.getParams());
            }
        }

        @Override
        @Nonnull
        public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
            return this.server.completeCommands("effek", args, sender, server);
        }
    }

    private static class EventHandler {
        private final EfsServerImpl server;
        EventHandler(EfsServerImpl server) {
            this.server = server;
        }

        @SubscribeEvent
        public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            server.receiveEvent(new EfsPlayerEvent.Join(event.player.getUniqueID()));
        }

        @SubscribeEvent
        public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            server.receiveEvent(new EfsPlayerEvent.Leave(event.player.getUniqueID()));
        }

        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START && event.side == Side.SERVER && event.type == TickEvent.Type.SERVER)
                server.receiveEvent(new EfsServerEvent.Tick<>(FMLCommonHandler.instance().getMinecraftServerInstance()));
        }
    }
}
