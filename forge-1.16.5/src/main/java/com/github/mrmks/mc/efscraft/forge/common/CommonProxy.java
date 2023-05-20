package com.github.mrmks.mc.efscraft.forge.common;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
    private File configurationFolder;

    public CommonProxy(String modVersion) {
        this.modVersion = modVersion;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        this.wrapper = new NetworkWrapper();
        MinecraftForge.EVENT_BUS.register(new EventHandlerImpl(compatibleClients, wrapper));
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        File file;
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server.isDedicatedServer()) {
            // on dedicated server, read registries in ./configs/efscraft/effects.json
            file = new File(new File(configurationFolder, "efscraft"), "effects.json");
        } else {
            // on integrated server, read registries in ./saves/<world>/efscraft/effects.json
            file = new File(new File(server.getFile(server.getWorldData().getLevelName()), "efscraft"), "effects.json");
        }


    }

}
