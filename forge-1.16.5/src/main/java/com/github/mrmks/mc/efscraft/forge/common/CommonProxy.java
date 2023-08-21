package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import com.github.mrmks.mc.efscraft.server.EfsServerEnv;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.server.event.EfsServerEvent;
import com.mojang.brigadier.context.CommandContext;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommonProxy {

    protected static final Logger LOGGER = LogManager.getLogger("efscraft");

    protected final NetworkWrapper wrapper;
    protected final LogAdaptor logAdaptor = LogAdaptor.of(LOGGER);
    private final EfsServerImpl efsServer;

    public CommonProxy(String modVersion) {

        this.wrapper = new NetworkWrapper();

        EfsServerAdaptorImpl adaptor = new EfsServerAdaptorImpl(wrapper);
        efsServer = new EfsServerImpl(adaptor, logAdaptor, EfsServerEnv.FORGE, modVersion, false);

        this.wrapper.setServer(efsServer);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandler(efsServer));
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
    }

    public void onServerAboutToStart(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        List<File> files = new ArrayList<>();
        File file;
        file = new File(new File(FMLPaths.CONFIGDIR.get().toFile().getAbsoluteFile(), "efscraft"), "effects.json");

        files.add(file);

        if (!server.isDedicatedServer()) {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json
            file = new File(server.getWorldPath(new FolderName("efscraft")).toFile(), "effects.json");
            files.add(file);
        }

        efsServer.receiveEvent(new EfsServerEvent.Start<>(server, files));

        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        efsServer.receiveEvent(EfsServerEvent.Stop.INSTANCE);
    }

    public void registerCommand(RegisterCommandsEvent event) {
        CommandAdaptor.register(new CommandAdaptor(efsServer), event.getDispatcher());
    }

    private static class EfsServerImpl extends EfsServer<MinecraftServer, ServerWorld, Entity, ServerPlayerEntity, CommandContext<CommandSource>, ByteBufOutputStream> {
        public EfsServerImpl(EfsServerAdaptorImpl adaptor, LogAdaptor logger, EfsServerEnv env, String implVer, boolean autoReply) {
            super(adaptor, logger, env, implVer, autoReply);
        }
    }

    private static class EventHandler {

        private final EfsServer<MinecraftServer, ?, ?, ?, ?, ?> efsServer;

        EventHandler(EfsServer<MinecraftServer, ?, ?, ?, ?, ?> efsServer) {
            this.efsServer = efsServer;
        }

        @SubscribeEvent
        public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            efsServer.receiveEvent(new EfsPlayerEvent.Join(event.getPlayer().getUUID()));
        }

        @SubscribeEvent
        public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            efsServer.receiveEvent(new EfsPlayerEvent.Leave(event.getPlayer().getUUID()));
        }

        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {

                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null)
                    efsServer.receiveEvent(new EfsServerEvent.Tick<>(server));
            }
        }
    }
}
