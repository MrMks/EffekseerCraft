package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.IMessageHandler;
import com.github.mrmks.mc.efscraft.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.packet.MessageContext;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

class NetworkWrapper implements PluginMessageListener {

    private final MessageCodec codec = new MessageCodec();

    private final Plugin plugin;
    NetworkWrapper(Plugin plugin) {
        this.plugin = plugin;
    }

    <T extends IMessage> void register(Class<T> klass, IMessageHandler<T, ? extends IMessage> handler) {
        codec.register(klass, handler);
    }

    void sendTo(Player player, IMessage packet) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutput output = ByteStreams.newDataOutput(stream);
        try {
            codec.writeOutput(packet, output);
            player.sendPluginMessage(plugin, Constants.CHANNEL_KEY, stream.toByteArray());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        // receive a client packet;
        if (Constants.CHANNEL_KEY.equals(s)) {
            DataInput input = new DataInputStream(new ByteArrayInputStream(bytes));
            try {
                // convert it to Packet
                IMessage out = codec.writeInput(input, new MessageContext(player.getUniqueId()));
                // send out reply;
                if (out != null) sendTo(player, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
