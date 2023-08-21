package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.client.IEfsClientAdaptor;
import com.github.mrmks.mc.efscraft.client.event.EfsDisconnectEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import com.github.mrmks.mc.efscraft.math.Matrix4f;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.File;

public class ClientProxy extends CommonProxy {
    @Override
    public void initialize(FMLInitializationEvent event) {

        super.initialize(event);

        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {

            Minecraft mc = Minecraft.getMinecraft();

            // create efs client
            RendererImpl renderer = new RendererImpl();
            EfsClientAdaptorImpl adaptor = new EfsClientAdaptorImpl(wrapper, renderer);
            EfsClientImpl efsClient = new EfsClientImpl(adaptor, logAdaptor, false, new File(mc.getResourcePackRepository().getDirResourcepacks(), "efscraft"));

            wrapper.setClient(efsClient);

            // add resource loader to load effects;
//            ResourceManager resources = new ResourceManager(logAdaptor);
            ((IReloadableResourceManager) mc.getResourceManager())
                    .registerReloadListener(any -> efsClient.receiveEvent(EfsResourceEvent.Reload.INSTANCE));

            // create program container, used to delete the program when thread exit;
            // the program is required to be deleted on the same thread where it was created, or program
            // will exit with some resources unreleased or with some exceptions print in logs.

            // on 1.12.2, it uses the same thread of main thread, so
            // we let Minecraft call us when the thread about to exit;
            // such a function is completed by runtime bytecode transform;
//            EfsDrawingQueue<?> queue = new EfsDrawingQueue<>(resources::get, new EntityConvertImpl(), logAdaptor);
//            RendererImpl renderer = new RendererImpl(queue);
            MinecraftForge.EVENT_BUS.register(new EventHandler(efsClient));

            // register callbacks
//            ClientEventHooks.registerCleanup(resources::cleanup);
//            ClientEventHooks.registerCleanup(renderer::deleteProgram);
//            ClientEventHooks.registerCleanup(renderer::deleteFramebuffer);
            ClientEventHooks.registerCleanup(efsClient::deleteAll);
            ClientEventHooks.registerCleanup(renderer::deleteFramebuffer);
            ClientEventHooks.registerCleanup(EffekSeer4J::finish);

            // register packet handlers;
//            MessageHandlerClient client = new MessageHandlerClient(queue, ClientProxy::scheduleTask);
//            wrapper.registerClient(PacketHello.class, new PacketHello.ClientHandler(client::handleHello));
//            wrapper.registerClient(SPacketPlayWith.class, client::handlePlayWith);
//            wrapper.registerClient(SPacketPlayAt.class, client::handlePlayAt);
//            wrapper.registerClient(SPacketStop.class, client::handleStop);
//            wrapper.registerClient(SPacketClear.class, client::handleClear);
//            wrapper.registerClient(SPacketTrigger.class, client::handleTrigger);
        }
    }

//    static void scheduleTask(Runnable task) {
//        Minecraft mc = Minecraft.getMinecraft();
//        boolean sync = mc.isCallingFromMinecraftThread();
//        if (sync) task.run(); else mc.addScheduledTask(task);
//    }

    private static class EfsClientImpl extends EfsClient<Entity, EntityPlayerSP, ByteBufOutputStream> {
        public EfsClientImpl(IEfsClientAdaptor<Entity, EntityPlayerSP, ByteBufOutputStream> adaptor, LogAdaptor logger, boolean autoReply, File folder) {
            super(adaptor, logger, autoReply, folder);
        }
    }

    private static class EventHandler {

        EfsClient<?,?,?> client;

        EventHandler(EfsClient<?,?,?> client) {
            this.client = client;
        }

        @SubscribeEvent
        public void renderWorld(RendererImpl.RenderParticleEvent event) {
            Matrix4f matProj = new Matrix4f(RendererImpl.getProjectionMatrix());
            Matrix4f matModel = new Matrix4f(RendererImpl.getModelviewMatrix());

            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            Vec3f vPos = entity == null ? new Vec3f() : new Vec3f(entity.posX, entity.posY, entity.posZ);
            Vec3f vPrev = entity == null ? new Vec3f() : new Vec3f(entity.prevPosX, entity.prevPosY, entity.prevPosZ);

            EfsRenderEvent efsRenderEvent = new EfsRenderEvent(
                    event.partial,
                    event.finishNano,
                    Minecraft.getMinecraft().isGamePaused(),
                    matProj,
                    matModel, vPos, vPrev,
                    event.prev ? EfsRenderEvent.Phase.START : EfsRenderEvent.Phase.END
            );

            client.receiveEvent(efsRenderEvent);
        }

        @SubscribeEvent
        public void clientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            Minecraft.getMinecraft().addScheduledTask(() -> client.receiveEvent(EfsDisconnectEvent.INSTANCE));
        }
    }

}
