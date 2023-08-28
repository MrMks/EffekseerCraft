package com.github.mrmks.mc.efscraft.forge.client;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.client.event.EfsDisconnectEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;
import com.github.mrmks.mc.efscraft.common.IEfsEvent;
import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import com.github.mrmks.mc.efscraft.forge.common.CommonProxy;
import com.github.mrmks.mc.efscraft.math.Vec3f;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.File;

import static com.github.mrmks.mc.efscraft.forge.client.GLHelper.FLOAT_16;

public class ClientProxy extends CommonProxy {

    public static RenderLevelStageEvent.Stage BEFORE_DEBUG;
    public static RenderLevelStageEvent.Stage BEFORE_TRANSPARENCY;

    public ClientProxy(String version) {
        super(version);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onRegisterStage);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        event.enqueueWork(this::syncCommonSetup);
    }

    private void onRegisterStage(RenderLevelStageEvent.RegisterStageEvent event) {
        BEFORE_DEBUG = event.register(new ResourceLocation("efscraft", "before_debug"), null);
        BEFORE_TRANSPARENCY = event.register(new ResourceLocation("efscraft", "before_transparency"), null);
    }

    private void syncCommonSetup() {
        if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {
            Minecraft mc = Minecraft.getInstance();

            RendererImpl renderer = new RendererImpl();
            File folder = new File(mc.getResourcePackDirectory(), "efscraft");
            EfsClientImpl client = new EfsClientImpl(wrapper, renderer, logAdaptor, false, folder);

            ResourceManagerReloadListener listener = manager -> client.receiveEvent(EfsResourceEvent.Reload.INSTANCE);
            ((ReloadableResourceManager) mc.getResourceManager()).registerReloadListener(listener);

            MinecraftForge.EVENT_BUS.register(new EventHandler(client));

            EffekseerCraft.registerCleanup(client::deleteAll);
            EffekseerCraft.registerCleanup(renderer::closeResources);
            EffekseerCraft.registerCleanup(EffekSeer4J::finish);
        }
    }

    static class EventHandler {

        final EfsClientImpl client;
        EventHandler(EfsClientImpl client) {
            this.client = client;
        }

        @SubscribeEvent
        public void renderWorldStage(RenderLevelStageEvent event) {

            IEfsEvent.Phase phase = null;

            if (event.getStage() == BEFORE_TRANSPARENCY) {
                // client.receiveClient(); // START
                phase = IEfsEvent.Phase.START;
            } else if (event.getStage() == BEFORE_DEBUG) {
                // client.receiveClient(); // END
                phase = IEfsEvent.Phase.END;
            }

            if (phase == null) return;

            Minecraft minecraft = Minecraft.getInstance();
            com.github.mrmks.mc.efscraft.math.Matrix4f matView, matProj;
            Vec3f vPos;
            {
                float[] floats = new float[16];
                FLOAT_16.clear();
                event.getPoseStack().last().pose().store(FLOAT_16);
                FLOAT_16.get(floats);
                matView = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

                FLOAT_16.clear();
                event.getProjectionMatrix().store(FLOAT_16);
                FLOAT_16.get(floats);
                matProj = new com.github.mrmks.mc.efscraft.math.Matrix4f(floats);

                Camera camera = minecraft.gameRenderer.getMainCamera();
                Vec3 vec3 = camera.getPosition();
                vPos = new Vec3f(vec3.x, vec3.y, vec3.z);
            }

            matView.translatef(vPos.negative());

            client.receiveEvent(new EfsRenderEvent(event.getPartialTick(), Util.getNanos(), minecraft.isPaused(), matProj, matView, phase));
        }

        @SubscribeEvent
        public void clientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
            client.receiveEvent(EfsDisconnectEvent.INSTANCE);
        }
    }
}
