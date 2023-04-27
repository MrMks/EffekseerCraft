package com.github.mrmks.mc.efscraft.spigot;

import com.github.mrmks.mc.efscraft.Constants;
import com.github.mrmks.mc.efscraft.packet.IMessage;
import com.github.mrmks.mc.efscraft.packet.MessageCodec;
import com.github.mrmks.mc.efscraft.packet.MessageContext;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

class MessengeCodecAdaptor extends MessageCodec implements PluginMessageListener {

    private final Plugin plugin;
    MessengeCodecAdaptor(Plugin plugin) {
        this.plugin = plugin;
    }

    void sendTo(Player player, IMessage packet) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutput output = ByteStreams.newDataOutput(stream);
        try {
            writeOutput(packet, output);
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
                IMessage out = writeInput(input, new MessageContext(player.getUniqueId()));
                // send out reply;
                if (out != null) sendTo(player, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
