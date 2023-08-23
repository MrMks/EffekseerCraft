package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.server.event.EfsPlayerEvent;
import com.github.mrmks.mc.efscraft.server.event.EfsServerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommonProxy {

    protected static final Logger LOGGER = LogManager.getLogger("efscraft");
    protected final NetworkWrapper wrapper = new NetworkWrapper();
    protected final LogAdaptor logAdaptor = LogAdaptor.of(LOGGER);
    private final EfsServerImpl efsServer;

    public CommonProxy(String version) {
        efsServer = new EfsServerImpl(wrapper, logAdaptor, version);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandler(efsServer));
        MinecraftForge.EVENT_BUS.addListener(this::onGatherPermissionNode);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    public void onGatherPermissionNode(PermissionGatherEvent.Nodes event) {
        PermissionNode<Boolean> node = new PermissionNode<>("efscraft", "command", PermissionTypes.BOOLEAN, ((p, u, c) -> p == null || p.server.getPlayerList().isOp(p.getGameProfile()) ? Boolean.TRUE : Boolean.FALSE));
        event.addNodes(node);
    }

    public void onServerStarting(ServerStartingEvent event) {

        List<File> files = new ArrayList<>();

        File file, confDir = new File(FMLPaths.CONFIGDIR.get().toFile().getAbsoluteFile(), "efscraft");
        file = new File(confDir, "effects.json");

        files.add(file);

        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) {
            file = new File(server.getWorldPath(new LevelResource("efscraft")).toFile(), "effects.json");
            files.add(file);
        }

        file = new File(confDir, "decrypts.json");

        efsServer.receiveEvent(new EfsServerEvent.Start<>(server, files, file));
    }

    public void onServerStopping(ServerStoppingEvent event) {
        efsServer.receiveEvent(EfsServerEvent.Stop.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandAdaptor.register(new CommandAdaptor(efsServer), event.getDispatcher());
    }

    static class EventHandler {

        private final EfsServerImpl server;
        EventHandler(EfsServerImpl server) {
            this.server = server;
        }

        @SubscribeEvent
        public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            server.receiveEvent(new EfsPlayerEvent.Join(event.getPlayer().getUUID()));
        }

        @SubscribeEvent
        public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            server.receiveEvent(new EfsPlayerEvent.Leave(event.getPlayer().getUUID()));
        }

        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null)
                    this.server.receiveEvent(new EfsServerEvent.Tick<>(server));
            }
        }
    }
}
