package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.packet.PacketHello;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonProxy {
    protected NetworkWrapper wrapper;
    protected transient boolean versionCompatible = false;
    private final Map<UUID, PacketHello.State> compatibleClients = new ConcurrentHashMap<>();
    private String modVersion;
    private File configurationFolder;
    protected Logger logger;
    protected final boolean DEBUG_VERBOSE = System.getProperty("efscraft.debug_verbose", "false").equals("true");

    public void preInitialize(FMLPreInitializationEvent event) {
        configurationFolder = event.getModConfigurationDirectory();
        modVersion = event.getModMetadata().version;
        logger = event.getModLog();
    }

    public void initialize(FMLInitializationEvent event) {
        this.wrapper = new NetworkWrapper();
        // handler of message hello
        this.wrapper.register(PacketHello.class, new PacketHello.Handler(it -> versionCompatible = it, compatibleClients));
        MinecraftForge.EVENT_BUS.register(new EventHandlerImpl(wrapper, compatibleClients));
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        File file;
        MinecraftServer server = event.getServer();
        if (server.isDedicatedServer()) {
            // on dedicated server, read registries in ./configs/efscraft/effects.json
            file = new File(new File(configurationFolder, "efscraft"), "effects.json");
        } else {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json
            file = new File(server.getActiveAnvilConverter().getFile(server.getFolderName(), "efscraft"), "effects.json");
        }
        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");
        event.registerServerCommand(new CommandAdaptor(wrapper, file, compatibleClients, modVersion));
    }

    public void serverStarted(FMLServerStartedEvent event) {
    }

    public void serverStopping(FMLServerStoppingEvent event) {
    }

    public void serverStopped(FMLServerStoppedEvent event) {
    }

}
