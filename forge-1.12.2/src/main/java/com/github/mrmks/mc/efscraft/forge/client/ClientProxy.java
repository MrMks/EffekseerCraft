package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import com.github.mrmks.mc.efscraft.packet.SPacketClear;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayAt;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;
import com.github.mrmks.mc.efscraft.packet.SPacketStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void initialize(FMLInitializationEvent event) {

        super.initialize(event);

        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {

            Minecraft mc = Minecraft.getMinecraft();

            // add resource loader to load effects;
            ResourceManager resources = new ResourceManager();
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);

            // create program container, used to delete the program when thread exit;
            // the program is required to be deleted on the same thread where it was created, or program
            // will exit with some resources unreleased or with some exceptions print in logs.

            // on 1.12.2, it uses the same thread of main thread, so
            // we let Minecraft call us when the thread about to exit;
            // such a function is completed by runtime bytecode transform;
            RenderQueue queue = new RenderQueue(resources::get);
            Renderer renderer = new Renderer(queue);
            MinecraftForge.EVENT_BUS.register(renderer);

            // register callbacks
            EffekseerCraft.registerCleanup(resources::cleanup);
            EffekseerCraft.registerCleanup(renderer::deleteProgram);
            EffekseerCraft.registerCleanup(EffekSeer4J::finish);

            // register packet handlers;
            NetHandlerClient client = new NetHandlerClient(this, queue);
            wrapper.register(SPacketPlayWith.class, client::handlePlayWith);
            wrapper.register(SPacketPlayAt.class, client::handlePlayAt);
            wrapper.register(SPacketStop.class, client::handleStop);
            wrapper.register(SPacketClear.class, client::handleClear);
        }
    }

    boolean isVersionCompatible() {
        return versionCompatible;
    }
}
