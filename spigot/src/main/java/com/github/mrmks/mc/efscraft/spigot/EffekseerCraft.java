package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import io.netty.util.internal.ConcurrentSet;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * We will not provide api for this, please use commands instead;
 */
public class EffekseerCraft extends JavaPlugin {

    private final boolean forgeDetected;
//    private EffectRegistry registry;

    public EffekseerCraft() {
        boolean flag = false;
        try {
            // it is possible that we are used as a plugin along with forge, but it is non-necessary;
            // we are only accessible via commands, so it is not important about who register those commands.
            Class.forName("com.github.mrmks.mc.efscraft.forge.EfsCraft");
            flag = true;
        } catch (ClassNotFoundException e) {}
        forgeDetected = flag;
    }

    @Override
    public void onLoad() {
        if (forgeDetected) return;
    }

    @Override
    public void onEnable() {
        if (forgeDetected) return;

        MessageCodecAdaptor network = new MessageCodecAdaptor(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, network);

        ConcurrentSet<UUID> clients = new ConcurrentSet<>();

        network.register(PacketHello.class, (packetIn, context) -> {
            if (packetIn.getVersion() == Constants.PROTOCOL_VERSION)
                clients.add(context.getSender());
            return null;
        });

        Localize localize = new Localize();
        try (InputStream stream = getResource("lang/en_us.lang")) {
            localize.onLoad(stream);
        } catch (IOException e) {
            getLogger().warning("Unable to load lang/en_us.lang");
        }

        getCommand("effek").setExecutor(new CommandAdaptor(this, network, clients, localize));

        EventListener listener = new EventListener(network, clients);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, listener::tick, 0, 0);
    }

    @Override
    public void onDisable() {
        if (forgeDetected) return;
    }
}
