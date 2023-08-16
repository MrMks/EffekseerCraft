package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;
import com.github.mrmks.mc.efscraft.common.IEfsEvent;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.FLOAT_16;

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

            RendererImpl renderer = new RendererImpl();
            EfsClientAdaptorImpl adaptor = new EfsClientAdaptorImpl(wrapper, renderer);
            EfsClient<Entity, ClientPlayerEntity, ByteBufInputStream, ByteBufOutputStream> client = new EfsClient<>(adaptor, logAdaptor, false);

            wrapper.setClient(client);

            ISelectiveResourceReloadListener reloadListener = (manager, pred) -> client.receiveEvent(EfsResourceEvent.Reload.INSTANCE);
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(reloadListener);

//            EfsDrawingQueue<?> queue = new EfsDrawingQueue<>(resources::get, new EfsClientAdaptorImpl(), logAdaptor);
//            RendererImpl renderer = new RendererImpl(queue);
//            MinecraftForge.EVENT_BUS.register(renderer);
            MinecraftForge.EVENT_BUS.register(new EventHandler(client));

//            EffekseerCraft.registerCleanup(resources::cleanup);
            EffekseerCraft.registerCleanup(client::deleteAll);
            EffekseerCraft.registerCleanup(renderer::cleanup);
            EffekseerCraft.registerCleanup(EffekSeer4J::finish);

//            MessageHandlerClient client = new MessageHandlerClient(queue, ClientProxy::scheduleTask);
//            wrapper.registerClient(PacketHello.class, new PacketHello.ClientHandler(client::handleHello));
//            wrapper.registerClient(SPacketPlayWith.class, client::handlePlayWith);
//            wrapper.registerClient(SPacketPlayAt.class, client::handlePlayAt);
//            wrapper.registerClient(SPacketStop.class, client::handleStop);
//            wrapper.registerClient(SPacketClear.class, client::handleClear);
//            wrapper.registerClient(SPacketTrigger.class, client::handleTrigger);
        }
    }

    private static class EventHandler {
        EfsClient<?, ?, ?, ?> efsClient;
        EventHandler(EfsClient<?,?,?,?> client) {
            this.efsClient = client;
        }

        @SubscribeEvent
        public void onRenderEvent(RenderWorldLastEvent event) {
            if (Minecraft.useFancyGraphics()) return;

            float[] floats = new float[16];
            FLOAT_16.clear();
            event.getMatrixStack().last().pose().store(FLOAT_16);
            FLOAT_16.get(floats);
            com.github.mrmks.mc.efscraft.math.Matrix4f matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            FLOAT_16.clear();
            event.getProjectionMatrix().store(FLOAT_16);
            FLOAT_16.get(floats);
            com.github.mrmks.mc.efscraft.math.Matrix4f matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            ActiveRenderInfo info = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vector3d v3d = info.getPosition();
            Vec3f vPos = new Vec3f(v3d.x, v3d.y, v3d.z);

            Minecraft mc = Minecraft.getInstance();

            matProj.translatef(vPos.negative());

            float partial = event.getPartialTicks();
            long nanoNow = event.getFinishTimeNano();

            efsClient.receiveEvent(new EfsRenderEvent(partial, nanoNow, mc.isPaused(), matProj, matView, IEfsEvent.Phase.START));
            efsClient.receiveEvent(new EfsRenderEvent(partial, nanoNow, mc.isPaused(), matProj, matView, IEfsEvent.Phase.END));
        }

        @SubscribeEvent
        public void onRenderEvent(RendererImpl.RenderParticleEvent event) {
            if (!Minecraft.useFancyGraphics()) return;

            com.github.mrmks.mc.efscraft.math.Matrix4f matView, matProj;
            Vec3f vPos;

            float[] floats = new float[16];
            FLOAT_16.clear();
            event.cam.store(FLOAT_16);
            FLOAT_16.get(floats);
            matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            FLOAT_16.clear();
            event.proj.store(FLOAT_16);
            FLOAT_16.get(floats);
            matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

            Vector3d v3d = event.info.getPosition();
            vPos = new Vec3f(v3d.x, v3d.y, v3d.z);

            matView.translatef(vPos.negative());

            efsClient.receiveEvent(new EfsRenderEvent(event.partial, event.nano, Minecraft.getInstance().isPaused(), matProj, matView, event.prev ? IEfsEvent.Phase.START : IEfsEvent.Phase.END));
        }
    }
}
