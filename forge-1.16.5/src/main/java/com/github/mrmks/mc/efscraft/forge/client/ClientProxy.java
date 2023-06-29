package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.MessageHandlerClient;
import com.github.mrmks.mc.efscraft.client.Renderer;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import com.github.mrmks.mc.efscraft.common.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy extends CommonProxy {
    public ClientProxy(String modVersion) {
        super(modVersion);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        event.enqueueWork(this::syncCommonSetup);
    }

    private void syncCommonSetup() {
        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {
            Minecraft mc = Minecraft.getInstance();

            ResourceManager resources = new ResourceManager(logAdaptor);
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);
            resources.onResourceManagerReload(mc.getResourceManager(), it -> true);

            RenderingQueue queue = new RenderingQueue(resources::get, new EntityConvertImpl(), logAdaptor);
            Renderer renderer = new RendererImpl(queue);
            MinecraftForge.EVENT_BUS.register(renderer);

            EffekseerCraft.registerCleanup(resources::cleanup);
            EffekseerCraft.registerCleanup(renderer::deleteProgram);
            EffekseerCraft.registerCleanup(EffekSeer4J::finish);

            MessageHandlerClient client = new MessageHandlerClient(queue);
            wrapper.registerClient(PacketHello.class, new PacketHello.ClientHandler(client::handleHello));
            wrapper.registerClient(SPacketPlayWith.class, client::handlePlayWith);
            wrapper.registerClient(SPacketPlayAt.class, client::handlePlayAt);
            wrapper.registerClient(SPacketStop.class, client::handleStop);
            wrapper.registerClient(SPacketClear.class, client::handleClear);
            wrapper.registerClient(SPacketTrigger.class, client::handleTrigger);
        }
    }

}
