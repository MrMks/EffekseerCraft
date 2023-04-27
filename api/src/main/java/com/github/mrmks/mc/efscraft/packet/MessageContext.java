package com.github.mrmks.mc.efscraft.packet;

import java.util.UUID;

public class MessageContext {

    private final boolean remote;
    private final UUID sender;
    public MessageContext(UUID sender) {
        this.remote = sender == null;
        this.sender = sender;
    }

    // are we handling a packet on remote? are we handling a packet from server?
    public boolean isRemote() {
        return remote;
    }
    // if we are on server(received a packet from client), this is the uuid of the sender;
    public UUID getSender() {
        return sender;
    }
}
