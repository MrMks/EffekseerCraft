package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.common.packet.MessageContext;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
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

import java.io.DataInput;
import java.io.IOException;

public class NetworkWrapper extends MessageCodec {

    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.tryParse(Constants.CHANNEL_KEY);
    private static final Logger LOGGER = LogManager.getLogger("efscraft");

    NetworkWrapper() {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                CHANNEL_NAME,
                () -> String.valueOf(Constants.PROTOCOL_VERSION),
                any -> true,
                any -> true
        );

        channel.addListener(this::receivePacket);
    }

    private void receivePacket(NetworkEvent event) {
        if (event instanceof NetworkEvent.ChannelRegistrationChangeEvent)
            return;

        NetworkEvent.Context context = event.getSource().get();
        context.setPacketHandled(true);

        MessageContext ctx = new MessageContext(context.getSender() == null ? null : context.getSender().getUUID());

        FriendlyByteBuf buffer = event.getPayload();
        DataInput input = new ByteBufInputStream(buffer);
        NetworkPacket reply = null;
        try {
            reply = super.readInput(input, ctx);
            if (ctx.isRemote()) buffer.release();
        } catch (IOException e) {
            LOGGER.error("Unable to decode and handle a message", e);
        }

        if (reply != null) {
            context.getPacketDispatcher().sendPacket(CHANNEL_NAME, toBuffer(reply).getLeft());
        }
    }

    public void sendTo(Player player, NetworkPacket message) {
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> (ServerPlayer) player);
        target.send(toVanillaPacket(target.getDirection(), message));
    }

    private Pair<FriendlyByteBuf, Integer> toBuffer(NetworkPacket message) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            if (!super.writeOutput(message, new ByteBufOutputStream(buffer))) {
                buffer.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to encode message: " + message.getClass(), e);
        }

        return Pair.of(buffer, Integer.MIN_VALUE);
    }

    private Packet<?> toVanillaPacket(NetworkDirection direction, NetworkPacket message) {
        return direction.buildPacket(toBuffer(message), CHANNEL_NAME).getThis();
    }

}
