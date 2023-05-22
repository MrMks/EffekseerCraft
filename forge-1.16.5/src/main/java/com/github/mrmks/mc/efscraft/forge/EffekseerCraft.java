package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.mc.efscraft.forge.client.ClientProxy;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;

@Mod("efscraft")
public class EffekseerCraft {

    private static final List<Runnable> callbacks = new ArrayList<>();
    public static void callbackCleanup() {
        callbacks.forEach(Runnable::run);
        callbacks.clear();
    }

    public static void registerCleanup(Runnable runnable) {
        callbacks.add(runnable);
    }

    private final CommonProxy proxy;

    public EffekseerCraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        IModInfo info = ModLoadingContext.get().getActiveContainer().getModInfo();

        Dist dist = FMLEnvironment.dist;
        CommonProxy proxy = null;

        if (dist.isClient()) {
            proxy = new ClientProxy(info.getVersion().toString());
        } else if (dist.isDedicatedServer()) {
            proxy = new CommonProxy(info.getVersion().toString());
        }

        this.proxy = proxy;

        if (proxy != null) {
            bus.addListener(proxy::onCommonSetup);
        }
    }
}
