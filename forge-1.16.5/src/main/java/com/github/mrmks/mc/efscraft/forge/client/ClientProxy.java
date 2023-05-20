package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy extends CommonProxy {
    public ClientProxy(String modVersion) {
        super(modVersion);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        event.enqueueWork(ClientProxy::syncCommonSetup);
    }

    private static void syncCommonSetup() {
        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {
            Minecraft mc = Minecraft.getInstance();

            ResourceManager resources = new ResourceManager(LOGGER);
            EffekseerCraft.registerCleanup(resources::cleanup);
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);
            resources.onResourceManagerReload(mc.getResourceManager(), it -> true);

            RenderingQueue queue = new RenderingQueue(resources::get, new EntityConvertImpl());
            Renderer renderer = new RendererImpl(queue);
            MinecraftForge.EVENT_BUS.register(renderer);


        }
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }
}
