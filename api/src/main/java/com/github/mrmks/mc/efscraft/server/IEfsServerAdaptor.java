package com.github.mrmks.mc.efscraft.server;

import com.github.mrmks.mc.efscraft.common.IEfsNetworkAdaptor;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 *
 * @param <SV> Server
 * @param <WO> World
 * @param <EN> Entity
 * @param <PL> Player
 * @param <SE> Sender
 * @param <DO> DataOutput
 */
public interface IEfsServerAdaptor<SV, WO, EN, PL extends EN, SE, DO extends OutputStream>
    extends IEfsNetworkAdaptor<DO> {

    // test if a sender has some permission.
    boolean hasPermission(SV sv, SE sender, String permissionNode);

    PL getPlayer(SV sv, UUID uuid);

    /**
     * @param toFind a string fo indicate which entity to find, we always try UUID version of this method first, therefore, if this method is called, this will not be a uuid;
     * @return null if entity can not be found in given world
     */
    EN findEntity(SV sv, SE sender, String toFind) throws EfsServerCommandHandler.CommandException;
    PL findPlayer(SV sv, SE sender, String toFind) throws EfsServerCommandHandler.CommandException;

    /**
     * @return null if entity can not be found in given world
     */
    EN getEntity(SV sv, WO world, UUID uuid);

    /**
     * @return null if entity is null or can not be cast to a instance of PL;
     */
    PL getPlayerEntity(EN entity);
    EN getEntitySender(SE sender);

    /**
     * @return null if desired world can not be found;
     */
    WO getWorld(SV sv, String world) throws EfsServerCommandHandler.CommandException;
    WO getWorld(SV sv, SE sender, String world) throws EfsServerCommandHandler.CommandException;

    int getEntityId(EN entity);
    UUID getEntityUUID(EN entity);
    WO getEntityWorld(EN entity);
    Vec3f getEntityPos(EN entity);
    Vec2f getEntityAngle(EN entity);
    String getPlayerName(PL player);
    String getWorldName(WO world);
    int getWorldViewDistance(WO world);
    Vec3f getSenderPos(SE sender);

    List<WO> getWorlds(SV sv);
    List<WO> getWorlds(SV sv, SE sender);
    List<PL> getPlayersInWorld(WO world);
    List<PL> getPlayersInServer(SV sv);
    List<PL> getPlayersInServer(SV sv, SE sender);

    DO createOutput();
    void closeOutput(DO output) throws IOException;
    void sendPacket(Collection<PL> players, Predicate<PL> test, DO output);
    void sendPacket(SV sv, Collection<PL> players, Predicate<PL> test, DO output);

    void sendMessage(SV sv, SE sender, String msg, Object[] args, boolean scheduled);
}
