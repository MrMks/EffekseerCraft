package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.ILogAdaptor;
import com.github.mrmks.mc.efscraft.packet.PacketHello;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

        ILogAdaptor adaptor = new LogAdaptor(getLogger());
        NetworkWrapper network = new NetworkWrapper(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, network);

        Map<UUID, PacketHello.State> clients = new ConcurrentHashMap<>();

        network.register(PacketHello.class, new PacketHello.Handler(clients, adaptor));

        Localize localize = new Localize();
        try (InputStream stream = getResource("lang/en_us.lang")) {
            localize.onLoad(stream);
        } catch (IOException e) {
            getLogger().warning("Unable to load lang/en_us.lang");
        }

        getCommand("effek").setExecutor(new CommandAdaptor(this, network, clients, localize));

        EventHandlerImpl listener = new EventHandlerImpl(this, network, clients, adaptor);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTaskTimer(this, listener::tick, 0, 0);
        try {
            getServer().getPluginManager().registerEvents(listener.channelListener(), this);
        } catch (NoClassDefFoundError error) {
            getServer().getPluginManager().registerEvents(listener.loginListener(), this);
        }
    }

    @Override
    public void onDisable() {
        if (forgeDetected) return;
    }
}
