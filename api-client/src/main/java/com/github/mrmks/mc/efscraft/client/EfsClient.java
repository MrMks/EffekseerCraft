package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EfsClient<EN, PL extends EN, DI extends DataInput, DO extends DataOutput> {
    final LogAdaptor logger;
    final IEfsClientAdaptor<EN, PL, DI, DO> adaptor;

    final EfsResourceManager resources;
    final EfsRenderer renderer;

    final EfsClientPacketHandler<DI, DO> packetHandler;
    final EfsClientEventHandler eventHandler;

    public EfsClient(IEfsClientAdaptor<EN, PL, DI, DO> adaptor, LogAdaptor logger, boolean autoReply) {
        this.logger = logger;
        this.adaptor = adaptor;

        this.resources = new EfsResourceManager(this);
        this.renderer = new EfsRenderer();

        this.packetHandler = new EfsClientPacketHandler<>(this, autoReply);
        this.eventHandler = new EfsClientEventHandler(this);
    }

    public NetworkPacket receivePacket(DI dataInput) throws IOException {
        return packetHandler.receive(dataInput);
    }

    public void receiveEvent(IEfsClientEvent event) {

    }
}
