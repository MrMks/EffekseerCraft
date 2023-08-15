package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

@Mod(
        modid = "efscraft",
        name = "EffekseerCraft",
        acceptableRemoteVersions = "*",
        useMetadata = true
)
public class EffekseerCraft {

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
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }
}
