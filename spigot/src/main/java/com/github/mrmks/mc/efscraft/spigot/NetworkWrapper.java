package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.common.Constants;
import com.github.mrmks.mc.efscraft.common.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.common.packet.MessageContext;
import com.github.mrmks.mc.efscraft.common.packet.NetworkPacket;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.logging.Level;

class NetworkWrapper implements PluginMessageListener {

    private final MessageCodec codec = new MessageCodec();

    private final Plugin plugin;
    NetworkWrapper(Plugin plugin) {
        this.plugin = plugin;
    }

    <T extends NetworkPacket> void register(Class<T> klass, NetworkPacket.ServerHandler<T, ? extends NetworkPacket> handler) {
        codec.registerServer(klass, handler);
    }

    void sendTo(Player player, NetworkPacket packet) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutput output = ByteStreams.newDataOutput(stream);
        try {
            codec.writeOutput(packet, output);
            player.sendPluginMessage(plugin, Constants.CHANNEL_KEY, stream.toByteArray());
            stream.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Exception while send message(" + packet.getClass().getTypeName() + ") to a client", e);
        }
    }

    @Override
    public void onPluginMessageReceived(@Nonnull String s, @Nonnull Player player, @Nonnull byte[] bytes) {
        // receive a client packet;
        if (Constants.CHANNEL_KEY.equals(s)) {
            DataInput input = new DataInputStream(new ByteArrayInputStream(bytes));
            try {
                // convert it to Packet
                NetworkPacket out = codec.readInput(input, new MessageContext(player.getUniqueId()));
                // send out reply;
                if (out != null) sendTo(player, out);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Exception while receiving message from client(" + player.getName() + ")", e);
            }
        }
    }
}
