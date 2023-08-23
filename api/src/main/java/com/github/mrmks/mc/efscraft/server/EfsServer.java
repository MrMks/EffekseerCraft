package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.common.HandshakeState;
import com.github.mrmks.mc.efscraft.common.LogAdaptor;
import com.github.mrmks.mc.efscraft.common.packet.PacketHello;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EfsServer<SV, WO, EN, PL extends EN, SE, DO extends OutputStream> {

    final IEfsServerAdaptor<SV, WO, EN, PL, SE, DO> adaptor;
    final EfsServerCommandHandler<SV, WO, EN, PL, SE> commandHandler;
    final EfsServerPacketHandler<SV, EN, PL, DO> packetHandler;
    final EfsServerEventHandler<SV> eventHandler;

    final Map<UUID, HandshakeState> clients;
    final Map<String, byte[]> decryptKeys;
    protected final LogAdaptor logger;
    protected final EfsServerEnv env;
    protected final String implVer;

    public EfsServer(
            IEfsServerAdaptor<SV, WO, EN, PL, SE, DO> adaptor,
            LogAdaptor logger,
            EfsServerEnv env,
            String implVer,
            boolean autoReply
    ) {
        this.adaptor = adaptor;
        this.logger = logger;
        this.env = env;
        this.implVer = implVer;
        this.clients = new ConcurrentHashMap<>();
        this.decryptKeys = Collections.synchronizedMap(new HashMap<>());

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
    public DO receivePacket(SV sv, PL receiver, InputStream dataIn) throws IOException {
        return packetHandler.receive(sv, receiver, dataIn);
    }
}
