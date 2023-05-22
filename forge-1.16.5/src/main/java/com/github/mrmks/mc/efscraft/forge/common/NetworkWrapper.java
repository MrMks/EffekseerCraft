package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.IMessageHandler;
import com.github.mrmks.mc.efscraft.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.packet.MessageContext;
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
import java.util.function.Consumer;

public class NetworkWrapper {
    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.tryParse(Constants.CHANNEL_KEY);
    private static final Logger LOGGER = LogManager.getLogger("efscraft");
    private final MessageCodec codec;

    NetworkWrapper() {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(
                CHANNEL_NAME,
                () -> String.valueOf(Constants.PROTOCOL_VERSION),
                any -> true,
                any -> true);

        channel.addListener(this::receivePacket);

        this.codec = new MessageCodec();
    }

    public <T extends IMessage> void register(Class<T> klass, IMessageHandler<T, ? extends IMessage> handler) {
        codec.register(klass, handler);
    }

    public <T extends IMessage> void register(Class<T> klass, Consumer<T> handler) {
        register(klass, (packetIn, context) -> {
            handler.accept(packetIn); return null;
        });
    }

    private void receivePacket(NetworkEvent event) {

        if (event instanceof NetworkEvent.ChannelRegistrationChangeEvent)
            return;

        NetworkEvent.Context context = event.getSource().get();
        context.setPacketHandled(true);

        MessageContext ctx = new MessageContext(context.getSender() == null ? null : context.getSender().getUUID());

        PacketBuffer buffer = event.getPayload();
        DataInput input = new ByteBufInputStream(buffer);
        IMessage reply = null;
        try {
            reply = codec.writeInput(input, ctx);
            buffer.release();
        } catch (IOException e) {
            LOGGER.error("Unable to decode and handle a message", e);
        }

        if (reply != null) {
            context.getPacketDispatcher().sendPacket(CHANNEL_NAME, toBuffer(reply).getLeft());
        }
    }

    public void sendTo(PlayerEntity player, IMessage message) {
        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player);
        target.send(toVanillaPacket(target.getDirection(), message));
    }

    private Pair<PacketBuffer, Integer> toBuffer(IMessage message) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            if (!codec.writeOutput(message, new ByteBufOutputStream(buffer))) {
                buffer.release();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to encode message: " + message.getClass(), e);
        }

        return Pair.of(buffer, Integer.MIN_VALUE);
    }

    private IPacket<?> toVanillaPacket(NetworkDirection direction, IMessage message) {
        return direction.buildPacket(toBuffer(message), CHANNEL_NAME).getThis();
    }
}
