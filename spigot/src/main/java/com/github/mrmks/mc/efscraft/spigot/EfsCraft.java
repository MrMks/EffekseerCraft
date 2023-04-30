package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.EffectRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * We will not provide api for this, please use commands instead;
 */
public class EfsCraft extends JavaPlugin {

    private final boolean forgeDetected;
    private EffectRegistry registry;

    public EfsCraft() {
        boolean flag = false;
        try {
            // it is possible that we are used as a plugin along with forge, but it is non-necessary;
            // we are only accessible via commands, so it is not important about who register those commands.
            Class.forName("com.github.mrmks.mc.efscraft.forge.EfsCraft");
            flag = true;
        } catch (ClassNotFoundException e) {}
        forgeDetected = flag;
        getPluginLoader().disablePlugin(this);
    }

    @Override
    public void onLoad() {
        if (forgeDetected) return;
        // load effects registry here, maybe pending to sub-thread? We can use CompleteFutures here;
        File file = new File(getDataFolder(), "effects.json");
        registry = new EffectRegistry(file);
        registry.reload(() -> {});
    }

    @Override
    public void onEnable() {
        if (forgeDetected) return;

        MessageCodecAdaptor listener = new MessageCodecAdaptor(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Constants.CHANNEL_KEY);
        getServer().getMessenger().registerIncomingPluginChannel(this, Constants.CHANNEL_KEY, listener);

        CommandExecutor executor = new CommandExecutor(this, registry, listener);
        getCommand("effek").setExecutor(executor);
    }

    @Override
    public void onDisable() {
        if (forgeDetected) return;
    }
}
