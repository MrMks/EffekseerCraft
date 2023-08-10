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

public class EfsServer<EN, PL extends EN, WO, SE, DO extends DataOutput, DI extends DataInput, CTX> {

    protected final IEfsServerAdaptor<EN, PL, WO, SE, DO, DI, CTX> adaptor;
    protected final EfsCommandHandler<EN, PL, CTX, SE, WO> commandHandler;
    protected final EfsPacketHandler<EN, PL, DO, DI, CTX> packetHandler;
    protected final EfsEventHandler eventHandler;

    final Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();
    protected final LogAdaptor logger;
    protected final EfsServerEnv env;
    protected final String implVer;

    public EfsServer(
            IEfsServerAdaptor<EN, PL, WO, SE, DO, DI, CTX> adaptor,
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
    public void executeCommands(String label, String[] args, SE sender, CTX ctx) throws EfsCommandHandler.CommandException {
        commandHandler.dispatchExecute(label, args, ctx, sender);
    }

    public List<String> completeCommands(String label, String[] args, SE sender, CTX ctx) {
        return commandHandler.dispatchComplete(label, args, ctx, sender);
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
