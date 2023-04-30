package com.github.mrmks.mc.efscraft.forge;

import com.github.mrmks.efkseer4j.EffekSeer4J;
import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.EffectRegistry;
import com.github.mrmks.mc.efscraft.packet.*;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.command.CommandTreeBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
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
    public void preInitialize(FMLPreInitializationEvent event) {
        proxy.preInitialize(event);
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        proxy.initialize(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        proxy.serverAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }

    public static class CommonProxy {
        protected NetworkWrapper wrapper;
        protected boolean versionCompatible = false;
        private final Set<UUID> compatibleClients = new ConcurrentSet<>();
        private File configurationFolder;

        void preInitialize(FMLPreInitializationEvent event) {
            configurationFolder = event.getModConfigurationDirectory();
        }

        void initialize(FMLInitializationEvent event) {
            this.wrapper = new NetworkWrapper();
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
            MinecraftForge.EVENT_BUS.register(new ServerEventListener(this));
        }

        void serverAboutToStart(FMLServerAboutToStartEvent event) {}

        void serverStarting(FMLServerStartingEvent event) {
            File file;
            MinecraftServer server = event.getServer();
            if (server.isDedicatedServer()) {
                file = new File(new File(configurationFolder, "efscraft"), "effects.json");
            } else {
                file = new File(server.getActiveAnvilConverter().getFile(server.getFolderName(), "efscraft"), "effects.json");
            }
            EffectRegistry registry = new EffectRegistry(file);
            event.registerServerCommand(new CommandEffek(this, registry));
        }

        void serverStarted(FMLServerStartedEvent event) {}

        void serverStopping(FMLServerStoppingEvent event) {}

        void serverStopped(FMLServerStoppedEvent event) {}

        // next two methods will be invoked from command handler or server event handler;
        boolean isClientCompatible(UUID uuid) {
            return compatibleClients.contains(uuid);
        }

        void logoutClient(UUID uuid) {
            compatibleClients.remove(uuid);
        }

    }

    public static class ClientProxy extends CommonProxy {
        @Override
        void initialize(FMLInitializationEvent event) {

            super.initialize(event);

            if (EffekSeer4J.setup(EffekSeer4J.Device.OPENGL)) {

                Minecraft mc = Minecraft.getMinecraft();

                // add resource loader to load effects;
                ResourceManager resources = new ResourceManager();
                ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(resources);

                // create program container, used to delete the program when thread exit;
                // the program is required to be deleted on the same thread where it was created, or program
                // will exit with some resources unreleased or with some exceptions print in logs.

                // on 1.12.2, it uses the same thread of main thread, so
                // we let Minecraft call us when the thread about to exit;
                // such a function is completed by runtime bytecode transform;
                MessageQueue queue = new MessageQueue(resources::get);
                Renderer renderer = new Renderer(queue);
                MinecraftForge.EVENT_BUS.register(renderer);

                // register callbacks
                callbacks.add(renderer::deleteProgram);
                callbacks.add(EffekSeer4J::finish);

                // register packet handlers;
                wrapper.register(SPacketPlayWith.class, (packetIn, context) -> {
                    if (versionCompatible) queue.processWith(packetIn);
                    return null;
                });
                wrapper.register(SPacketPlayAt.class, (packetIn, context) -> {
                    if (versionCompatible) queue.processAt(packetIn);
                    return null;
                });
                wrapper.register(SPacketStop.class, (packetIn, context) -> {
                    if (versionCompatible) queue.processStop(packetIn);
                    return null;
                });
                wrapper.register(SPacketClear.class, (packetIn, context) -> {
                    if (versionCompatible) queue.processClear(packetIn);
                    return null;
                });
            }
        }
    }

    private static class ServerEventListener {
        private final Map<UUID, CountDown> counter = new HashMap<>();
        private final CommonProxy proxy;

        ServerEventListener(CommonProxy proxy) {
            this.proxy = proxy;
        }

        @SubscribeEvent
        public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            UUID uuid = event.player.getPersistentID();
            counter.put(uuid, new CountDown(10));
        }

        @SubscribeEvent
        public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            UUID uuid = event.player.getPersistentID();
            counter.remove(uuid);

            proxy.compatibleClients.remove(uuid);
        }

        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {

                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                if (server == null) return;
                PlayerList list = server.getPlayerList();

                Iterator<Map.Entry<UUID, CountDown>> iterator = counter.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, CountDown> entry = iterator.next();
                    if (entry.getValue().update()) {
                        iterator.remove();
                        EntityPlayer player = list.getPlayerByUUID(entry.getKey());

                        proxy.wrapper.sendTo(new PacketHello(), player);
                    }
                }
            }
        }
    }


    private static class CountDown {
        int current;

        CountDown(int init) {
            current = init;
        }

        boolean update() {
            current--;
            return current <= 0;
        }
    }

    private static class NetworkWrapper {

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
                IMessage packet = codec.writeInput(stream, new MessageContext(uuid));
                stream.close();

                if (packet != null) {
                    out.add(packet);
    //                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
    //                ctx.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            }
        }

        private final MessageCodec codec = new MessageCodec();
        private final EnumMap<Side, FMLEmbeddedChannel> channels;
        NetworkWrapper() {
            channels = NetworkRegistry.INSTANCE.newChannel(Constants.CHANNEL_KEY, new MessageCodecAdaptor(codec));
            channels.get(Side.CLIENT).pipeline().addLast(new SimpleChannelInboundHandler<IMessage>(IMessage.class) {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, IMessage msg) throws Exception {
                    if (msg != null) {
                        ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
                        ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }
            });
        }

        <T extends IMessage> void register(Class<T> klass, IMessageHandler<T, ? extends IMessage> handler) {
            codec.register(klass, handler);
        }

        void sendTo(IMessage message, EntityPlayer player) {
            FMLEmbeddedChannel channel = channels.get(Side.SERVER);

            channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
            channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
            channel.writeAndFlush(message).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    private static class CommandEffek extends CommandTreeBase {

        private final CommonProxy proxy;
        private final EffectRegistry registry;

        CommandEffek(CommonProxy proxy, EffectRegistry registry) {
            this.proxy = proxy;
            this.registry = registry;

            addSubcommand(new Adaptor("reload", this::executeReload));
            addSubcommand(new Adaptor("play", this::executePlay, this::compilePlay));
            addSubcommand(new Adaptor("stop", this::executeStop));
            addSubcommand(new Adaptor("clear", this::executeClear));
            addSubcommand(new Adaptor("version", this::executeVersion));
        }

        @Override @Nonnull
        public String getName() {
            return "effek";
        }

        @Override @Nonnull
        public String getUsage(@Nonnull ICommandSender sender) {
            return "commands.effek.usage";
        }

        void executeReload(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            WeakReference<ICommandSender> senderRef = new WeakReference<>(sender);
            registry.reload(() -> server.addScheduledTask(() -> {
                ICommandSender s = senderRef.get();
                if (s != null) {
                    s.sendMessage(new TextComponentTranslation("commands.effek.reload.success"));
                }
            }));
        }

        void executePlay(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
//            sender.sendMessage(new TextComponentString(Arrays.toString(args)));
            if (args.length < 2) {
                sender.sendMessage(new TextComponentTranslation("commands.effek.play.arg_length_0"));
            } else {
                String effect = args[0], emitter = args[1];
                if (!registry.isExist(effect)) {
                    sender.sendMessage(new TextComponentTranslation("commands.effek.play.effect_absent"));
                } else {

                    World world = null;
                    ChunkPos chunkPos = null;
                    IMessage message = null;

                    if (args.length > 5) {
                        world = DimensionManager.getWorld(parseInt(args[2]));
                        double x = parseDouble(args[3], -30000000, 30000000);
                        double y = parseDouble(args[4], -30000000, 30000000);
                        double z = parseDouble(args[5], -30000000, 30000000);

                        BlockPos blockPos = new BlockPos(x, y, z);
                        chunkPos = new ChunkPos(blockPos);

                        if (world == null) {
                            sender.sendMessage(new TextComponentTranslation("commands.effek.play.world_absent"));
                        } else {
                            message = registry.createPlayAt(effect, emitter, x, y, z);
                        }
                    } else if (args.length > 2) {
                        Entity entity = getEntity(server, sender, args[2]);
                        world = entity.world;
                        chunkPos = new ChunkPos(new BlockPos(entity.posX, entity.posY, entity.posZ));
                        UUID uuid = entity.getPersistentID();

                        message = registry.createPlayWith(effect, emitter, uuid);
                    }

                    if (world == null || message == null) {
                        sender.sendMessage(new TextComponentTranslation("commands.effek.play.non_satisfied_args"));
                        return;
                    }

                    List<EntityPlayer> players = world.playerEntities;
                    for (EntityPlayer player : players) {
                        BlockPos p = new BlockPos(player.posX, player.posY, player.posZ);
                        ChunkPos cp = new ChunkPos(p);

                        if (Math.abs(cp.x - chunkPos.x) <= 10 && Math.abs(cp.z - chunkPos.z) <= 10) {
                            proxy.wrapper.sendTo(message, player);
                        }
                    }
                }
            }
        }

        List<String> compilePlay(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
            if (args.length < 3) return Collections.emptyList();

            if (args.length == 3) {
                ArrayList<String> list = new ArrayList<>();

                for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                    list.add(player.getName());
                }

                Entity entity = sender.getCommandSenderEntity();
//                if (entity != null && pos != null) {
//                    entity.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos)).forEach(it -> list.add(it.getPersistentID().toString()));
//                }

                return getListOfStringsMatchingLastWord(args, list);
            } else {
                return Collections.emptyList();
            }
        }

        void executeStop(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            sender.sendMessage(new TextComponentString(Arrays.toString(args)));

        }

        void executeClear(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            sender.sendMessage(new TextComponentString(Arrays.toString(args)));

        }

        void executeVersion(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            sender.sendMessage(new TextComponentString(Arrays.toString(args)));

        }

        private interface LambdaExec {
            void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;
        }

        private interface LambdaComp {
            List<String> compile(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos);
        }

        private static class Adaptor extends CommandBase {
            private final String name;
            private final LambdaExec exec;
            private final LambdaComp comp;
            protected Adaptor(String name, LambdaExec exec) {
                this.name = name;
                this.exec = exec;
                this.comp = null;
            }

            protected Adaptor(String name, LambdaExec exec, LambdaComp comp) {
                this.name = name;
                this.exec = exec;
                this.comp = comp;
            }

            @Override @Nonnull
            public String getName() {
                return name;
            }

            @Override @Nonnull
            public String getUsage(@Nonnull ICommandSender sender) {
                return "commands.effek." + name + ".usage";
            }

            @Override
            public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
                exec.execute(server, sender, args);
            }

            @Override @Nonnull
            public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
                return comp == null ? super.getTabCompletions(server, sender, args, targetPos) : comp.compile(server, sender, args, targetPos);
            }
        }
    }
}
