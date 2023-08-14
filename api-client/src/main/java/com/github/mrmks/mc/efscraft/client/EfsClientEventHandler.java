package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.client.event.EfsResourceEvent;

public class EfsClientEventHandler {

    private final EfsClient<?, ?, ?, ?> client;

    EfsClientEventHandler(EfsClient<?,?,?,?> client) {
        this.client = client;
    }

    void receive(IEfsClientEvent event) {
        if (event instanceof EfsRenderEvent) {

        } else if (event instanceof EfsResourceEvent) {

        }
    }



}
