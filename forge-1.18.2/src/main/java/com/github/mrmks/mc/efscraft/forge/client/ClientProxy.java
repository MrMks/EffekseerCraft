package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.MessageHandlerClient;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClientProxy extends CommonProxy {

    public ClientProxy(String version) {
        super(version);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        event.enqueueWork(this::syncCommonSetup);
    }

    private void syncCommonSetup() {
        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {
            Minecraft mc = Minecraft.getInstance();

            ResourceManagerImpl resources = new ResourceManagerImpl(logAdaptor);
            ((ReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);
            resources.onResourceManagerReload(mc.getResourceManager());

            RenderingQueue<?> queue = new RenderingQueue<>(resources::get, new EntityConvertImpl(), logAdaptor);
            RendererImpl renderer = new RendererImpl(queue);
            MinecraftForge.EVENT_BUS.register(renderer);

            EffekseerCraft.registerCleanup(resources::cleanup);
            EffekseerCraft.registerCleanup(renderer::deleteProgram);
            EffekseerCraft.registerCleanup(renderer::closeResources);
            EffekseerCraft.registerCleanup(EffekSeer4J::finish);

            MessageHandlerClient client = new MessageHandlerClient(queue, ClientProxy::scheduleTask);
            wrapper.registerClient(PacketHello.class, new PacketHello.ClientHandler(client::handleHello));
            wrapper.registerClient(SPacketPlayWith.class, client::handlePlayWith);
            wrapper.registerClient(SPacketPlayAt.class, client::handlePlayAt);
            wrapper.registerClient(SPacketStop.class, client::handleStop);
            wrapper.registerClient(SPacketClear.class, client::handleClear);
            wrapper.registerClient(SPacketTrigger.class, client::handleTrigger);
        }
    }

    static void scheduleTask(Runnable task) {
        Minecraft.getInstance().submit(task);
    }
}
