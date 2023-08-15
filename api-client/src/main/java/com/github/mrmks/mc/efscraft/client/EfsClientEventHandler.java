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

            if (event instanceof EfsRenderEvent.Prev) {
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
