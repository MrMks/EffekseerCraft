package com.github.mrmks.mc.efscraft.forge.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonProxy {

    protected static final Logger LOGGER = LogManager.getLogger("efscraft");

    protected NetworkWrapper wrapper;
    protected transient boolean versionCompatible = false;
    private final Set<UUID> compatibleClients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final String modVersion;
    private File effectsFile;

    public CommonProxy(String modVersion) {
        this.modVersion = modVersion;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        this.wrapper = new NetworkWrapper();
        MinecraftForge.EVENT_BUS.register(new EventHandlerImpl(compatibleClients, wrapper));
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    public void onServerAboutToStart(FMLServerStartingEvent event) {
        File file;
        MinecraftServer server = event.getServer();
        if (server.isDedicatedServer()) {
            // on dedicated server, read registries in ./configs/efscraft/effects.json
            file = new File(new File(FMLPaths.CONFIGDIR.get().toFile().getAbsoluteFile(), "efscraft"), "effects.json");
        } else {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json
            file = new File(server.getWorldPath(new FolderName("efscraft")).toFile(), "effects.json");
        }

        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");
        new CommandAdaptor(wrapper, file, compatibleClients, modVersion).register(server.getCommands().getDispatcher());
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
    }

}
