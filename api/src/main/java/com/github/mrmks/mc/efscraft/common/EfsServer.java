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
    protected final EfsCommandHandler<SV, WO, EN, PL, SE> commandHandler;
    protected final EfsPacketHandler<SV, EN, PL, DO, DI> packetHandler;
    protected final EfsEventHandler eventHandler;

    final Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();
    protected final LogAdaptor logger;
    protected final EfsServerEnv env;
    protected final String implVer;

    public EfsServer(
            IEfsServerAdaptor<SV, WO, EN, PL, SE, DO, DI> adaptor,
            LogAdaptor logger,
            List<File> registries,
            EfsServerEnv env,
            String implVer,
            boolean autoReply
    ) {
        this.adaptor = adaptor;
        this.logger = logger;
        this.env = env;
        this.implVer = implVer;

        this.commandHandler = new EfsCommandHandler<>(this, registries);
        this.packetHandler = new EfsPacketHandler<>(this, autoReply);
        this.eventHandler = new EfsEventHandler(this);
    }

    // commands
    public void executeCommands(String label, String[] args, SE sender, SV server) throws EfsCommandHandler.CommandException {
        commandHandler.dispatchExecute(label, args, server, sender);
    }

    public List<String> completeCommands(String label, String[] args, SE sender, SV server) {
        return commandHandler.dispatchComplete(label, args, server, sender);
    }

    // events
    public void receiveEvent(IEfsEvent event) {
        eventHandler.receive(event);
    }

    // packets
    public void receivePacket(PL receiver, DI dataIn) throws IOException {
        packetHandler.receive(receiver, dataIn);
    }
}
