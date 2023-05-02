package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

import java.util.ArrayList;
import java.util.List;

@Mod(
        modid = "efscraft",
        name = "EffekseerCraft",
        acceptableRemoteVersions = "*"
)
public class EffekseerCraft {

    /**
     * The callback method, called from Minecraft#shutdownMinecraftApplet;
     * This behavior is provided by runtime bytecode transform;
     * So, do not change the method name.
     */
    private static final List<Runnable> callbacks = new ArrayList<>();
    public static void callbackCleanup() {
        callbacks.forEach(Runnable::run);
        callbacks.clear();
    }

    public static void registerCleanup(Runnable runnable) {
        callbacks.add(runnable);
    }

    @SidedProxy(
            clientSide = "com.github.mrmks.mc.efscraft.forge.client.ClientProxy",
            serverSide = "com.github.mrmks.mc.efscraft.forge.common.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        proxy.preInitialize(event);
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        proxy.initialize(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }

}
