package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.client.event.EfsDisconnectEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;

public class EfsClientEventHandler {

    private final EfsClient<?, ?, ?, ?> client;

    EfsClientEventHandler(EfsClient<?,?,?,?> client) {
        this.client = client;
    }

    void receive(IEfsClientEvent event) {
        if (event instanceof EfsRenderEvent) {
            EfsRenderEvent rEvent = (EfsRenderEvent) event;

            if (rEvent.getPhase() == EfsRenderEvent.Phase.START) {
                if (!rEvent.isGamePause())
                    client.renderer.update(rEvent.getPartial(), rEvent.getNanoNow(), rEvent.isGamePause(), rEvent.getMatProj(), rEvent.getMatModel());
            }

            client.adaptor.drawEffect(rEvent, client.renderer::draw);

        } else if (event instanceof EfsResourceEvent) {

            if (event instanceof EfsResourceEvent.Reload)
                client.resources.onReload();

        } else if (event instanceof EfsDisconnectEvent) {
            client.compatible = false;
            client.renderer.stopAll();
        }
    }



}
