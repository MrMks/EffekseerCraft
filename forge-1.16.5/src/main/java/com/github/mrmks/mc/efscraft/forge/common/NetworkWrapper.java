package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class NetworkWrapper {
    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.tryParse(Constants.CHANNEL_KEY);
    private static final Logger LOGGER = LogManager.getLogger("efscraft");

    private EfsServer<MinecraftServer, ?, ? super ServerPlayerEntity, ServerPlayerEntity, ?, ByteBufInputStream, ByteBufOutputStream> efsServer;
    private EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> efsClient;

    NetworkWrapper() {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                CHANNEL_NAME,
                () -> String.valueOf(Constants.PROTOCOL_VERSION),
                any -> true,
                any -> true);

        channel.addListener(this::receivePacket);
    }

    public void setServer(EfsServer<MinecraftServer, ?, ? super ServerPlayerEntity, ServerPlayerEntity, ?, ByteBufInputStream, ByteBufOutputStream> efsServer) {
        this.efsServer = efsServer;
    }

    public void setClient(EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> efsClient) {
        this.efsClient = efsClient;
    }

    private void receivePacket(NetworkEvent event) {

        if (event instanceof NetworkEvent.ChannelRegistrationChangeEvent)
            return;

        NetworkEvent.Context context = event.getSource().get();
        context.setPacketHandled(true);

        ServerPlayerEntity player = context.getSender();

        PacketBuffer buffer = event.getPayload();
        ByteBufInputStream input = new ByteBufInputStream(buffer);
        ByteBufOutputStream reply = null;
        try {
            if (player != null)
                reply = efsServer.receivePacket(player.server, player, input);
            else {
                reply = efsClient.receivePacket(input);
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

    public void sendTo(PlayerEntity player, ByteBuf buf) {
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player);
        target.send(toVanillaPacket(target.getDirection(), buf));
    }

    public void sendTo(ByteBuf buf) {
        PacketDistributor.PacketTarget target = PacketDistributor.SERVER.noArg();
        target.send(toVanillaPacket(target.getDirection(), buf));
    }

    private Pair<PacketBuffer, Integer> toBuffer(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf.copy());
        return Pair.of(buffer, Integer.MIN_VALUE);
    }

    private IPacket<?> toVanillaPacket(NetworkDirection direction, ByteBuf buf) {
        return direction.buildPacket(toBuffer(buf), CHANNEL_NAME).getThis();
    }
}
