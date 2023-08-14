package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

public interface IEfsClientAdaptor<EN, PL, DI extends DataInput, DO extends DataOutput> {

    // entity convert
    EN findEntity(int entityId);
    PL getPlayerEntity(EN entity);

    Vec3f getEntityPos(EN entity);
    Vec3f getEntityPrevPos(EN entity);

    Vec2f getEntityAngle(EN entity);
    Vec2f getEntityPrevAngle(EN entity);

    Vec2f getEntityHeadAngle(EN entity);
    Vec2f getEntityHeadPrevAngle(EN entity);

    Vec2f getEntityBodyAngle(EN entity);
    Vec2f getEntityBodyPrevAngle(EN entity);

    // resources
    InputStream loadResource(String namespace, String path) throws IOException;

    // packets
    DO createPacket();
    void sendPacket(DO dataOutput);
}
