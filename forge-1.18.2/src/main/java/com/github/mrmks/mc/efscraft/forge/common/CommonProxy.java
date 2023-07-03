package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.ILogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonProxy {

    protected static final Logger LOGGER = LogManager.getLogger("efscraft");

    protected final String version;

    protected final NetworkWrapper wrapper = new NetworkWrapper();
    protected final ILogAdaptor logAdaptor = new LogAdaptor(LOGGER);
    private final Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();

    public CommonProxy(String version) {
        this.version = version;

        wrapper.registerServer(PacketHello.class, new PacketHello.ServerHandler(clients, logAdaptor));
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandlerImpl(wrapper, clients, logAdaptor));
        MinecraftForge.EVENT_BUS.addListener(this::onGatherPermissionNode);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    public void onGatherPermissionNode(PermissionGatherEvent.Nodes event) {
        PermissionNode<Boolean> node = new PermissionNode<>("efscraft", "command", PermissionTypes.BOOLEAN, ((p, u, c) -> p == null || p.server.getPlayerList().isOp(p.getGameProfile()) ? Boolean.TRUE : Boolean.FALSE));
        event.addNodes(node);
    }

    public void onServerStarting(ServerStartingEvent event) {
        File file;
        MinecraftServer server = event.getServer();
        if (server.isDedicatedServer()) {
            file = new File(new File(FMLPaths.CONFIGDIR.get().toFile().getAbsoluteFile(), "efscraft"), "effects.json");
        } else {
            file = new File(server.getWorldPath(new LevelResource("efscraft")).toFile(), "efscraft.json");
        }

        new CommandAdaptor(wrapper, file, clients, version).register(server.getCommands().getDispatcher());
    }
}
