package com.github.mrmks.mc.efscraft.forge.common;

import com.github.mrmks.mc.efscraft.client.EfsClient;
import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.EfsServer;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
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
    private static class MessageCodecReply extends SimpleChannelInboundHandler<ByteBuf> {
        MessageCodecReply() {
            super(ByteBuf.class);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            if (msg != null) {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        }
    }

    @ChannelHandler.Sharable
    private class MessageCodec extends MessageToMessageCodec<FMLProxyPacket, ByteBuf> {

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();
            FMLProxyPacket proxy = new FMLProxyPacket(new PacketBuffer(msg), channel);
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
                if (efsServer != null)
                    outputStream = efsServer.receivePacket(((NetHandlerPlayServer) msg.handler()).player, inputStream);
                else
                    outputStream = null;
            }

            inputStream.close();

            if (outputStream != null)
                out.add(outputStream.buffer());
        }
    }

    private final EnumMap<Side, FMLEmbeddedChannel> channels;
    private EfsServer<?, ?, ? super EntityPlayerMP, EntityPlayerMP, ?, ByteBufOutputStream, ByteBufInputStream> efsServer;
    private EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> efsClient;

    NetworkWrapper() {
        channels = NetworkRegistry.INSTANCE.newChannel(Constants.CHANNEL_KEY, new MessageCodec(), new MessageCodecReply());
    }

    public void setClient(EfsClient<?, ?, ByteBufInputStream, ByteBufOutputStream> client) {
        this.efsClient = client;
    }

    public void setServer(EfsServer<?,?,? super EntityPlayerMP, EntityPlayerMP,?, ByteBufOutputStream, ByteBufInputStream> server) {
        this.efsServer = server;
    }

    public void sendTo(EntityPlayer player, NetworkPacket message) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendTo(EntityPlayer player, ByteBuf buf) {
        FMLEmbeddedChannel channel = channels.get(Side.SERVER);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeAndFlush(buf).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void sendTo(ByteBuf buf) {
        FMLEmbeddedChannel channel = channels.get(Side.CLIENT);

        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        channel.writeAndFlush(buf).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
