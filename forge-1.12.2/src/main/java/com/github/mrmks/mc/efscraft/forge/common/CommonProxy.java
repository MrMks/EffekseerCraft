package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.*;
import com.github.mrmks.mc.efscraft.common.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.common.event.EfsServerEvent;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import io.netty.buffer.ByteBufInputStream;
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
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");

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
    }

    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        List<File> files = new ArrayList<>();

        // on server, read registries in ./configs/efscraft/effects.json
        File file = new File(new File(configurationFolder, "efscraft"), "effects.json");
        files.add(file);
        if (!server.isDedicatedServer()) {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json, too
            file = new File(server.getActiveAnvilConverter().getFile(server.getFolderName(), "efscraft"), "effects.json");
            files.add(file);
        }

        efsServer.receiveEvent(new EfsServerEvent.Start<>(event.getServer(), files));

        event.registerServerCommand(new CommandHandler(efsServer));
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        efsServer.receiveEvent(EfsServerEvent.Stop.INSTANCE);
    }

    protected static class EfsServerImpl extends EfsServer<MinecraftServer, WorldServer,
            Entity, EntityPlayerMP, ICommandSender, ByteBufOutputStream, ByteBufInputStream> {

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
        public String getName() {
            return "effek";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/effek play|stop|trigger|clear [subs...]";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            try {
                this.server.executeCommands("effek", args, sender, server);
            } catch (EfsServerCommandHandler.CommandException e) {
                throw new CommandException(e.getMessage(), e.getParams());
            }
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
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
                server.receiveEvent(new EfsServerEvent.Tick<>(FMLServerHandler.instance().getServer()));
        }
    }
}
