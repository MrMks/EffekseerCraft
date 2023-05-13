package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.IMessageHandler;
import com.github.mrmks.mc.efscraft.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.packet.MessageContext;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageCodec;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER;

public class NetworkWrapper {

    @ChannelHandler.Sharable
    private static class MessageCodecAdaptor extends MessageToMessageCodec<FMLProxyPacket, IMessage> {

        private final MessageCodec codec;

        MessageCodecAdaptor(MessageCodec codec) {
            this.codec = codec;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);
            ctx.channel().attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<>());
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, IMessage msg, List<Object> out) throws Exception {

            String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();

            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
            if (!codec.writeOutput(msg, stream)) {
                buffer.release();
                stream.close();
                return;
            }
            stream.close();

            FMLProxyPacket proxy = new FMLProxyPacket(buffer, channel);
            WeakReference<FMLProxyPacket> ref = ctx.channel().attr(INBOUNDPACKETTRACKER).get().get();
            FMLProxyPacket old = ref == null ? null : ref.get();
            if (old != null) {
                proxy.setDispatcher(old.getDispatcher());
            }
            out.add(proxy);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {

            boolean isRemote = msg.getTarget() == Side.CLIENT;
            UUID uuid = isRemote ? null : ((NetHandlerPlayServer) msg.handler()).player.getPersistentID();

            ByteBufInputStream stream = new ByteBufInputStream(msg.payload(), true);
            IMessage packet = codec.writeInput(stream, new MessageContext(uuid));
            stream.close();

            if (packet != null) out.add(packet);
        }
    }

    @ChannelHandler.Sharable
    private static class MessageCodecReply extends SimpleChannelInboundHandler<IMessage> {
        MessageCodecReply() {
            super(IMessage.class);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, IMessage msg) throws Exception {
            if (msg != null) {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        }
    }

    private final MessageCodec codec = new MessageCodec();
    private final EnumMap<Side, FMLEmbeddedChannel> channels;

    NetworkWrapper() {
        channels = NetworkRegistry.INSTANCE.newChannel(Constants.CHANNEL_KEY, new MessageCodecAdaptor(codec), new MessageCodecReply());
    }

    public <T extends IMessage> void register(Class<T> klass, IMessageHandler<T, ? extends IMessage> handler) {
        codec.register(klass, handler);
    }

    public <T extends IMessage> void register(Class<T> klass, Consumer<T> handler) {
        register(klass, (packetIn, context) -> {
            handler.accept(packetIn); return null;
        });
    }

    public void sendTo(EntityPlayer player, IMessage message) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }


}
