package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.server.EfsServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageCodec;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;

import static net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER;

public class NetworkWrapper {

    @ChannelHandler.Sharable
    private static class MessageCodecReply extends SimpleChannelInboundHandler<BufCan> {
        MessageCodecReply() {
            super(BufCan.class);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, BufCan msg) {
            if (msg != null) {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        }
    }

    @ChannelHandler.Sharable
    private class MessageCodec extends MessageToMessageCodec<FMLProxyPacket, BufCan> {

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);
            ctx.channel().attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<>());
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, BufCan msg, List<Object> out) {
            String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();
            FMLProxyPacket proxy = new FMLProxyPacket(new PacketBuffer(msg.buf), channel);
            WeakReference<FMLProxyPacket> ref = ctx.channel().attr(INBOUNDPACKETTRACKER).get().get();
            FMLProxyPacket old = ref == null ? null : ref.get();
            if (old != null) {
                proxy.setDispatcher(old.getDispatcher());
            }
            out.add(proxy);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
            boolean toRemote = msg.getTarget() == Side.CLIENT;
            ByteBufInputStream inputStream = new ByteBufInputStream(msg.payload(), true);
            ByteBufOutputStream outputStream;
            if (toRemote) {
                outputStream = efsClient.receivePacket(inputStream);
            } else {
                NetHandlerPlayServer handler = (NetHandlerPlayServer) msg.handler();
                outputStream = efsServer.receivePacket(handler.player.world.getMinecraftServer(), handler.player, inputStream);
            }

            ctx.channel().attr(INBOUNDPACKETTRACKER).get().set(new WeakReference<>(msg));
            inputStream.close();

            if (outputStream != null) {
                out.add(new BufCan(outputStream.buffer()));
            }
        }
    }

    private static class BufCan {
        ByteBuf buf;

        BufCan(ByteBuf buf) { this.buf = buf; }
    }

    private final EnumMap<Side, FMLEmbeddedChannel> channels;
    private EfsServer<MinecraftServer, ?, ? super EntityPlayerMP, EntityPlayerMP, ?, ByteBufInputStream, ByteBufOutputStream> efsServer;
    private EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> efsClient;

    NetworkWrapper() {
        channels = NetworkRegistry.INSTANCE.newChannel(Constants.CHANNEL_KEY, new MessageCodec(), new MessageCodecReply());
    }

    public void setClient(EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> client) {
        this.efsClient = client;
    }

    public void setServer(EfsServer<MinecraftServer,?,? super EntityPlayerMP, EntityPlayerMP,?, ByteBufInputStream, ByteBufOutputStream> server) {
        this.efsServer = server;
    }

    public void sendTo(EntityPlayer player, ByteBuf buf) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);

        channel.writeAndFlush(new BufCan(buf.retainedSlice())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendTo(ByteBuf buf) {
        FMLEmbeddedChannel channel = channels.get(Side.CLIENT);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);

        channel.writeAndFlush(new BufCan(buf.retainedSlice())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
