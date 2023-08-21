package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class NetworkWrapper {

    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.tryParse(Constants.CHANNEL_KEY);
    private static final Logger LOGGER = LogManager.getLogger("efscraft");

    private EfsServer<MinecraftServer, ?, ? super ServerPlayer, ServerPlayer, ?, ByteBufOutputStream> server;
    private EfsClient<?, ?, ByteBufOutputStream> client;

    NetworkWrapper() {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                CHANNEL_NAME,
                () -> String.valueOf(Constants.PROTOCOL_VERSION),
                any -> true,
                any -> true
        );

        channel.addListener(this::receivePacket);
    }

    public void setServer(EfsServer<MinecraftServer, ?, ? super ServerPlayer, ServerPlayer, ?, ByteBufOutputStream> server) {
        this.server = server;
    }

    public void setClient(EfsClient<?, ?, ByteBufOutputStream> client) {
        this.client = client;
    }

    private void receivePacket(NetworkEvent event) {
        if (event instanceof NetworkEvent.ChannelRegistrationChangeEvent)
            return;

        NetworkEvent.Context context = event.getSource().get();
        context.setPacketHandled(true);

        ServerPlayer player = context.getSender();

        FriendlyByteBuf buffer = event.getPayload();
        ByteBufInputStream input = new ByteBufInputStream(buffer);
        ByteBufOutputStream reply = null;
        try {
            if (player != null) {
                reply = server.receivePacket(player.server, player, input);
            } else {
                reply = client.receivePacket(input);
                buffer.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to decode and handle a message", e);
        }

        if (reply != null) {
            context.getPacketDispatcher().sendPacket(CHANNEL_NAME, toBuffer(reply.buffer()).getLeft());
            reply.buffer().release();
        }
    }

    public void sendTo(Player player, ByteBuf message) {
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> (ServerPlayer) player);
        target.send(toVanillaPacket(target.getDirection(), message));
    }

    public void send(ByteBuf message) {
        PacketDistributor.PacketTarget target = PacketDistributor.SERVER.noArg();
        target.send(toVanillaPacket(target.getDirection(), message));
    }

    private Pair<FriendlyByteBuf, Integer> toBuffer(ByteBuf message) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(message.copy());
        return Pair.of(buffer, Integer.MIN_VALUE);
    }

    private Packet<?> toVanillaPacket(NetworkDirection direction, ByteBuf message) {
        return direction.buildPacket(toBuffer(message), CHANNEL_NAME).getThis();
    }

}
