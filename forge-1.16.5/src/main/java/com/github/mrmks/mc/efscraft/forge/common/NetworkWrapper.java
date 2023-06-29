package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.common.packet.MessageContext;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
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
                any -> true);

        channel.addListener(this::receivePacket);
    }

    private void receivePacket(NetworkEvent event) {

        if (event instanceof NetworkEvent.ChannelRegistrationChangeEvent)
            return;

        NetworkEvent.Context context = event.getSource().get();
        context.setPacketHandled(true);

        MessageContext ctx = new MessageContext(context.getSender() == null ? null : context.getSender().getUUID());

        PacketBuffer buffer = event.getPayload();
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

    public void sendTo(PlayerEntity player, NetworkPacket message) {
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player);
        target.send(toVanillaPacket(target.getDirection(), message));
    }

    private Pair<PacketBuffer, Integer> toBuffer(NetworkPacket message) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            if (!super.writeOutput(message, new ByteBufOutputStream(buffer))) {
                buffer.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to encode message: " + message.getClass(), e);
        }

        return Pair.of(buffer, Integer.MIN_VALUE);
    }

    private IPacket<?> toVanillaPacket(NetworkDirection direction, NetworkPacket message) {
        return direction.buildPacket(toBuffer(message), CHANNEL_NAME).getThis();
    }
}
