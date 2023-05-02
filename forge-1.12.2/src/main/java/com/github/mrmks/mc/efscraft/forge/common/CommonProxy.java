package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.EffectRegistry;
import com.github.mrmks.mc.efscraft.packet.IMessageHandler;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.io.File;
import java.util.Set;
import java.util.UUID;

public class CommonProxy {
    protected NetworkWrapper wrapper;
    protected transient boolean versionCompatible = false;
    private final Set<UUID> compatibleClients = new ConcurrentSet<>();
    private String modVersion;
    private File configurationFolder;

    public void preInitialize(FMLPreInitializationEvent event) {
        configurationFolder = event.getModConfigurationDirectory();
        modVersion = event.getModMetadata().version;
    }

    public void initialize(FMLInitializationEvent event) {
        this.wrapper = new NetworkWrapper();
        // handler of message hello
        IMessageHandler<PacketHello, ?> handler = (packetIn, context) -> {
            if (context.isRemote()) {
                versionCompatible = packetIn.getVersion() == Constants.PROTOCOL_VERSION;
                return versionCompatible ? new PacketHello() : null;
            } else {
                boolean flag = packetIn.getVersion() == Constants.PROTOCOL_VERSION;
                if (flag) {
                    compatibleClients.add(context.getSender());
                } else {
                    compatibleClients.remove(context.getSender());
                }

                return null;
            }
        };
        this.wrapper.register(PacketHello.class, handler);
        MinecraftForge.EVENT_BUS.register(new ServerEventListener(this));
    }

    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
    }

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
        EffectRegistry registry = new EffectRegistry(file);
        PermissionAPI.registerNode("efscraft.command", DefaultPermissionLevel.OP, "permissions to use efscraft's commands");
        event.registerServerCommand(new EffekCommands(wrapper, registry, modVersion, this::isClientCompatible));
    }

    public void serverStarted(FMLServerStartedEvent event) {
    }

    public void serverStopping(FMLServerStoppingEvent event) {
    }

    public void serverStopped(FMLServerStoppedEvent event) {
    }

    public NetworkWrapper getNetwork() {
        return wrapper;
    }

    // next two methods will be invoked from command handler or server event handler;
    public boolean isClientCompatible(UUID uuid) {
        return compatibleClients.contains(uuid);
    }

    public void logoutClient(UUID uuid) {
        compatibleClients.remove(uuid);
    }

}
