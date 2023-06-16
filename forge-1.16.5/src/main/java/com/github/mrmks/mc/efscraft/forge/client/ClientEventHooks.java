package com.github.mrmks.mc.efscraft.forge.client;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

public class ClientEventHooks {
    public static void dispatchRenderEvent(float pPartialTicks, long pFinishTimeNano, Matrix4f pModelview, Matrix4f pProjection, ActiveRenderInfo pActiveRenderInfo, boolean prev) {
        Event event = new RendererImpl.RenderParticleEvent(pPartialTicks, pFinishTimeNano, pModelview, pProjection, pActiveRenderInfo, prev);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
