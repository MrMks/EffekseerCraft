package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.client.event.EfsRenderEvent;
import com.github.mrmks.mc.efscraft.common.IEfsNetworkAdaptor;
import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.io.*;

public interface IEfsClientAdaptor<EN, PL, DO extends OutputStream>
    extends IEfsNetworkAdaptor<DO> {

    // entity convert
    EN findEntity(int entityId);
    PL getPlayerEntity(EN entity);
    boolean isAlive(EN entity);
    
    Vec3f getEntityPos(EN entity);
    Vec3f getEntityPrevPos(EN entity);

    Vec2f getEntityAngle(EN entity);
    Vec2f getEntityPrevAngle(EN entity);

    boolean canUseHead(EN entity);
    Vec2f getEntityHeadAngle(EN entity);
    Vec2f getEntityHeadPrevAngle(EN entity);

    boolean canUseBody(EN entity);
    Vec2f getEntityBodyAngle(EN entity);
    Vec2f getEntityBodyPrevAngle(EN entity);

    // resources
    @Deprecated // load resources by EfsResourceManager, not from Minecraft's ResourceManager
    InputStream loadResource(String namespace, String key, String path) throws IOException;

    // packets
    DO createOutput();
    void closeOutput(DO output) throws IOException;
    void sendPacket(DO dataOutput);

    // render impl
    void drawEffect(EfsRenderEvent event, Runnable drawer);

    // main thread task
    void schedule(Runnable runnable);
}
