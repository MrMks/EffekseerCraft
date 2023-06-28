package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.ILogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommonProxy {

    protected static final Logger LOGGER = LogManager.getLogger("efscraft");

    protected final NetworkWrapper wrapper;
    protected final ILogAdaptor logAdaptor = new LogAdaptor(LOGGER);
    protected transient boolean versionCompatible = false;
    private final Map<UUID, PacketHello.State> compatibleClients = new ConcurrentHashMap<>();
    private final String modVersion;

    public CommonProxy(String modVersion) {
        this.modVersion = modVersion;

        this.wrapper = new NetworkWrapper();
        wrapper.register(PacketHello.class, new PacketHello.Handler((flag) -> versionCompatible = flag, compatibleClients, logAdaptor));
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventHandlerImpl(wrapper, compatibleClients, logAdaptor));
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
}
