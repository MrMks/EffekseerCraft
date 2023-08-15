package com.github.mrmks.mc.efscraft.client;

import com.github.mrmks.mc.efscraft.math.Vec2f;
import com.github.mrmks.mc.efscraft.math.Vec3f;

@Deprecated
public interface EntityConvert<ENTITY> {

    /**
     * @return a instance of entity, or null if target entity is absent;
     */
    ENTITY findEntity(int entityId);

    /**
     * @return true if the entity is a valid entity that exist in the game; false otherwise;
     */
    boolean isValid(ENTITY entity);

    /**
     * @return true if the entity is valid AND alive in the game; false otherwise;
     */
    boolean isAlive(ENTITY entity);

    Vec3f getPosition(ENTITY entity);

    Vec3f getPrevPosition(ENTITY entity);

    Vec2f getRotation(ENTITY entity);

    Vec2f getPrevRotation(ENTITY entity);

    boolean canUseHead(ENTITY entity);

    Vec2f getHeadRotation(ENTITY entity);

    Vec2f getPrevHeadRotation(ENTITY entity);

    boolean canUseRender(ENTITY entity);

    Vec2f getRenderRotation(ENTITY entity);

    Vec2f getPrevRenderRotation(ENTITY entity);
}
