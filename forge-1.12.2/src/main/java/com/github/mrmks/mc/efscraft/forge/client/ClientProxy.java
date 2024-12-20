package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.MessageHandlerClient;
import com.github.mrmks.mc.efscraft.client.RenderingQueue;
import com.github.mrmks.mc.efscraft.common.packet.*;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void initialize(FMLInitializationEvent event) {

        super.initialize(event);

//        Configuration cfg = new Configuration(configFile);
//        boolean translucent = cfg.getBoolean("renderTranslucent", "client", true, "Apply some kind of translucent effects, this will significantly affect fps");
//        cfg.save();

        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {

            Minecraft mc = Minecraft.getMinecraft();

            // add resource loader to load effects;
            ResourceManager resources = new ResourceManager(logAdaptor);
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);

            // create program container, used to delete the program when thread exit;
            // the program is required to be deleted on the same thread where it was created, or program
            // will exit with some resources unreleased or with some exceptions print in logs.

            // on 1.12.2, it uses the same thread of main thread, so
            // we let Minecraft call us when the thread about to exit;
            // such a function is completed by runtime bytecode transform;
            RenderingQueue<?> queue = new RenderingQueue<>(resources::get, new EntityConvertImpl(), logAdaptor);
            RendererImpl renderer = new RendererImpl(queue);
            MinecraftForge.EVENT_BUS.register(renderer);

            // register callbacks
            ClientEventHooks.registerCleanup(resources::cleanup);
            ClientEventHooks.registerCleanup(renderer::deleteProgram);
            ClientEventHooks.registerCleanup(renderer::deleteFramebuffer);
            ClientEventHooks.registerCleanup(EffekSeer4J::finish);

            // register packet handlers;
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
        Minecraft mc = Minecraft.getMinecraft();
        boolean sync = mc.isCallingFromMinecraftThread();
        if (sync) task.run(); else mc.addScheduledTask(task);
    }

}
