package com.github.mrmks.mc.efscraft.common;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 *
 * @param <EN> Entity
 * @param <PL> Player
 * @param <WO> World
 * @param <SE> Sender
 * @param <DA> Data
 * @param <DI>
 * @param <CTX>
 */
public interface IEfsServerAdaptor<CTX, WO, EN, PL extends EN, SE, DA extends DataOutput, DI extends DataInput>
    extends IEfsNetworkAdaptor<DI, DA> {

    // test if a sender has some permission.
    boolean hasPermission(CTX ctx, SE sender, String permissionNode);

    PL getPlayer(CTX ctx, UUID uuid);

    /**
     * @param toFind a string fo indicate which entity to find, we always try UUID version of this method first, therefore, if this method is called, this will not be a uuid;
     * @return null if entity can not be found in given world
     */
    EN findEntity(CTX ctx, SE sender, String toFind) throws EfsServerCommandHandler.CommandException;
    PL findPlayer(CTX ctx, SE sender, String toFind) throws EfsServerCommandHandler.CommandException;

    /**
     * @return null if entity can not be found in given world
     */
    EN getEntity(CTX ctx, WO world, UUID uuid);

    /**
     * @return null if entity is null or can not be cast to a instance of PL;
     */
    PL getPlayerEntity(EN entity);
    EN getEntitySender(SE sender);

    /**
     * @return null if desired world can not be found;
     */
    WO getWorld(CTX ctx, String world) throws EfsServerCommandHandler.CommandException;
    WO getWorld(CTX ctx, SE sender, String world) throws EfsServerCommandHandler.CommandException;

    int getEntityId(EN entity);
    UUID getEntityUUID(EN entity);
    WO getEntityWorld(EN entity);
    Vec3f getEntityPos(EN entity);
    Vec2f getEntityAngle(EN entity);
    String getPlayerName(PL player);
    String getWorldName(WO world);
    int getWorldViewDistance(WO world);
    Vec3f getSenderPos(SE sender);

    List<WO> getWorlds(CTX ctx);
    List<WO> getWorlds(CTX ctx, SE sender);
    List<PL> getPlayersInWorld(WO world);
    List<PL> getPlayersInServer(CTX ctx);
    List<PL> getPlayersInServer(CTX ctx, SE sender);

    DA createOutput();
    void closeOutput(DA output) throws IOException;
    void sendPacket(Collection<PL> players, Predicate<PL> test, DA output);
    void sendPacket(CTX ctx, Collection<PL> players, Predicate<PL> test, DA output);

    void sendMessage(CTX ctx, SE sender, String msg, Object[] args, boolean scheduled);
//    void sendMessage(CTX ctx, PL player, String msg, Object[] args, boolean scheduled, boolean validPlayer);
}
