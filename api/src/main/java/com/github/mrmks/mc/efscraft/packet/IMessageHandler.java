package com.github.mrmks.mc.efscraft.packet;

public interface IMessageHandler<IN extends IMessage, OUT extends IMessage> {
    OUT handlePacket(IN packetIn, MessageContext context);

}
