package com.github.mrmks.mc.efscraft.forge.client;

import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public class ClientEventHooks {

    private static final List<Runnable> callbacks = new ArrayList<>();
    public static void callbackCleanup() {
        callbacks.forEach(Runnable::run);
        callbacks.clear();
    }

    static void registerCleanup(Runnable task) {
        callbacks.add(task);
    }

    public static void resizeFramebuffers() {
        MinecraftForge.EVENT_BUS.post(new RendererImpl.ResizeFramebufferEvent());
    }

    public static void dispatchRendererEventPrev(int pass, float partial, long finishNano) {
        MinecraftForge.EVENT_BUS.post(new RendererImpl.RenderParticleEvent(pass, partial, finishNano, true));
    }

    public static void dispatchRendererEventPost(int pass, float partial, long finishNano) {
        MinecraftForge.EVENT_BUS.post(new RendererImpl.RenderParticleEvent(pass, partial, finishNano, false));
    }
}
