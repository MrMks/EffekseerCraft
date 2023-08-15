package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.common.packet.PacketHello;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EfsServer<SV, WO, EN, PL extends EN, SE, DO extends DataOutput, DI extends DataInput> {

    protected final IEfsServerAdaptor<SV, WO, EN, PL, SE, DO, DI> adaptor;
    protected final EfsServerCommandHandler<SV, WO, EN, PL, SE> commandHandler;
    protected final EfsServerPacketHandler<SV, EN, PL, DO, DI> packetHandler;
    protected final EfsServerEventHandler<SV> eventHandler;

    final Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();
    protected final LogAdaptor logger;
    protected final EfsServerEnv env;
    protected final String implVer;

    public EfsServer(
            IEfsServerAdaptor<SV, WO, EN, PL, SE, DO, DI> adaptor,
            LogAdaptor logger,
            EfsServerEnv env,
            String implVer,
            boolean autoReply
    ) {
        this.adaptor = adaptor;
        this.logger = logger;
        this.env = env;
        this.implVer = implVer;

        this.commandHandler = new EfsServerCommandHandler<>(this);
        this.packetHandler = new EfsServerPacketHandler<>(this, autoReply);
        this.eventHandler = new EfsServerEventHandler<>(this);
    }

    // commands
    public void executeCommands(String label, String[] args, SE sender, SV server) throws EfsServerCommandHandler.CommandException {
        commandHandler.dispatchExecute(label, args, server, sender);
    }

    public List<String> completeCommands(String label, String[] args, SE sender, SV server) {
        return commandHandler.dispatchComplete(label, args, server, sender);
    }

    // events
    public void receiveEvent(IEfsServerEvent event) {
        eventHandler.receive(event);
    }

    // packets
    public DO receivePacket(PL receiver, DI dataIn) throws IOException {
        return packetHandler.receive(receiver, dataIn);
    }
}
