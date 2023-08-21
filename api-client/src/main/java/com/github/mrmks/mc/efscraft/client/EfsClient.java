package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;

import java.io.*;

public class EfsClient<EN, PL extends EN, DO extends OutputStream> {
    final LogAdaptor logger;
    final IEfsClientAdaptor<EN, PL, DO> adaptor;

    final EfsResourceManager resources;
    final EfsRenderer renderer;

    final EfsClientPacketHandler<DO> packetHandler;
    final EfsClientEventHandler eventHandler;

    boolean compatible = false;

    public EfsClient(IEfsClientAdaptor<EN, PL, DO> adaptor, LogAdaptor logger, boolean autoReply, File folder) {
        this.logger = logger;
        this.adaptor = adaptor;

        this.resources = new EfsResourceManager(this, folder);
        this.eventHandler = new EfsClientEventHandler(this);

        EfsDrawingQueue<EN> queue = new EfsDrawingQueue<>(this);
        this.renderer = new EfsRenderer(this, queue);
        this.packetHandler = new EfsClientPacketHandler<>(this, autoReply, queue);
    }

    public DO receivePacket(InputStream dataInput) throws IOException {
        return packetHandler.receive(dataInput);
    }

    public void receiveEvent(IEfsClientEvent event) {
        eventHandler.receive(event);
    }

    public void deleteAll() {
        resources.onReload();
        renderer.clearAll();
    }
}
