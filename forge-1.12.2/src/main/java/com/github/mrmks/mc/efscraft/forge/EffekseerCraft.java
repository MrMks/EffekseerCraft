package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.*;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.command.ICommand;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.*;

import static net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER;

@Mod(
        modid = "efscraft",
        name = "EffekseerCraft",
        acceptableRemoteVersions = "*"
)
public class EffekseerCraft {

    /**
     * The callback method, called from Minecraft#shutdownMinecraftApplet;
     * This behavior is provided by runtime bytecode transform;
     * So, do not change the method name.
     */
    private static final List<Runnable> callbacks = new ArrayList<>();
    public static void callbackCleanup() {
        callbacks.forEach(Runnable::run);
        callbacks.clear();
    }

    @SidedProxy(
            clientSide = "com.github.mrmks.mc.efscraft.forge.EffekseerCraft$ClientProxy",
            serverSide = "com.github.mrmks.mc.efscraft.forge.EffekseerCraft$CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        proxy.initialize(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {

    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ICommand command = null; // todo
        event.registerServerCommand(command);

        MinecraftForge.EVENT_BUS.register(new ServerEventListener());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {

    }

    public static class CommonProxy {
        protected MessageCodec wrapper;
        protected EnumMap<Side, FMLEmbeddedChannel> channels;
        protected boolean versionCompatible = false;
        private final Set<UUID> compatibleClients = new ConcurrentSet<>();

        void initialize(FMLInitializationEvent event) {
            this.wrapper = new MessageCodec();
            this.channels = NetworkRegistry.INSTANCE.newChannel(Constants.CHANNEL_KEY, new MessageCodecAdaptor(wrapper));

            // handler of message hello
            this.wrapper.register(PacketHello.class, (packetIn, context) -> {
                if (context.isRemote()) {
                    versionCompatible = packetIn.getVersion() == Constants.PROTOCOL_VERSION;
                    return versionCompatible ? new PacketHello() : null;
                } else {
                    boolean flag = packetIn.getVersion() == Constants.PROTOCOL_VERSION;
                    if (flag) {
                        compatibleClients.add(context.getSender());
                    } else {
                        compatibleClients.remove(context.getSender());
                    }

                    return null;
                }
            });
        }

        // next two methods will be invoked from command handler or server event handler;
        boolean isClientCompatible(UUID uuid) {
            return compatibleClients.contains(uuid);
        }

        void logoutClient(UUID uuid) {
            compatibleClients.remove(uuid);
        }
    }

    public static class ClientProxy extends CommonProxy {

        private EffectRenderer renderer;

        @Override
        void initialize(FMLInitializationEvent event) {

            super.initialize(event);
            initializeEffekseer();

            // register packet handlers;
            if (renderer != null) {
                // todo implement packet handle;
                wrapper.register(SPacketPlayWith.class, (packetIn, context) -> null);
                wrapper.register(SPacketPlayAt.class, (packetIn, context) -> null);
                wrapper.register(SPacketStop.class, (packetIn, context) -> null);
                wrapper.register(SPacketClear.class, (packetIn, context) -> null);
            }
        }

        private void initializeEffekseer() {
            if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {

                Minecraft mc = Minecraft.getMinecraft();

                // add resource loader to load effects;
                EffectResourceManager effectReg = new EffectResourceManager();
                ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(effectReg);

                // create program container, used to delete the program when thread exit;
                // the program is required to be deleted on the same thread where it was created, or program
                // will exit with some resources unreleased, or with some exceptions print in logs.

                // on 1.12.2, it uses the same thread of main thread, so
                // we let Minecraft call us when the thread about to exit;
                // such a function is completed by runtime bytecode transform;
                EfsProgramContainer container = new EfsProgramContainer();
                renderer = new EffectRenderer(container, effectReg);
                MinecraftForge.EVENT_BUS.register(renderer);

                // register callbacks
                callbacks.add(container::delete);
                callbacks.add(effectReg::cleanUp);
            }

        }
    }

    private static class MessageCodecAdaptor extends MessageToMessageCodec<FMLProxyPacket, IMessage> {

        private final MessageCodec wrapper;
        MessageCodecAdaptor(MessageCodec wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, IMessage msg, List<Object> out) throws Exception {

            String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();

            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
            if (!wrapper.writeOutput(msg, stream)) {
                buffer.release();
                stream.close();
                return;
            }
            stream.close();

            FMLProxyPacket proxy = new FMLProxyPacket(buffer, channel);
            WeakReference<FMLProxyPacket> ref = ctx.channel().attr(INBOUNDPACKETTRACKER).get().get();
            FMLProxyPacket old = ref == null ? null : ref.get();
            if (old != null)
            {
                proxy.setDispatcher(old.getDispatcher());
            }
            out.add(proxy);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {

            boolean isRemote = msg.getTarget() == Side.CLIENT;
            UUID uuid = isRemote ? null : ((NetHandlerPlayServer) msg.handler()).player.getPersistentID();

            ByteBufInputStream stream = new ByteBufInputStream(msg.payload(), true);
            IMessage packet = wrapper.writeInput(stream, new MessageContext(uuid));
            stream.close();

            if (packet != null) {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                ctx.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }
        }
    }

    private static class ServerEventListener {

        @SubscribeEvent
        public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {}

        @SubscribeEvent
        public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {}
    }

}
