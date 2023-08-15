package com.github.mrmks.mc.efscraft.server.registry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class PojoV1 {

    public static class FollowArguments {
        @SerializedName("followX") public Boolean followX = null;
        @SerializedName("followY") public Boolean followY = null;
        @SerializedName("followZ") public Boolean followZ = null;
        @SerializedName("followYaw") public Boolean followYaw = null;
        @SerializedName("followPitch") public Boolean followPitch = null;
        @SerializedName("headDirection") public Boolean useHead = null;
        @SerializedName("bodyDirection") public Boolean useRender = null;
        @SerializedName("baseOnCurrentYaw") public Boolean inheritYaw = null;
        @SerializedName("baseOnCurrentPitch") public Boolean inheritPitch = null;
    }

    @SerializedName("extendsFrom") public String parent;
    @SerializedName("effect") public String effect;
    @SerializedName("lifespan") public Integer lifespan;
    @SerializedName("skipFrames") public  Integer skipFrame;
    @SerializedName("scales") public float[] scale;
    @SerializedName("dynamicInputs") public float[] dynamics = null;
    @SerializedName("localRotation") public float[] rotateLocal;
    @SerializedName("localTranslation") public float[] posLocal;
    @SerializedName("modelRotation") public float[] rotateModel;
    @SerializedName("modelTranslation") public float[] posModel;
    @SerializedName("overwriteConflict") public Boolean overwriteConflict = null;

    // properties for SPacketPlayWith
    @SerializedName("followArgs") public FollowArguments followArgs = null;

    static PojoV1 readFrom(Gson gson, JsonObject object, int ver) {
        if (ver > 1)
            throw new UnsupportedOperationException();
        else if (ver == 1)
            return gson.fromJson(object, PojoV1.class);
        else {
            PojoV0 p0 = PojoV0.readFrom(gson, object, ver);
            PojoV1 p1 = new PojoV1();

            p1.parent = p0.parent;
            p1.effect = p0.effect;
            p1.lifespan = p0.lifespan;
            p1.skipFrame = p0.skipFrame;
            p1.scale = p0.scale;
            p1.dynamics = p0.dynamics;
            p1.rotateLocal = p0.rotateLocal;
            p1.rotateModel = p0.rotateModel;
            p1.posLocal = p0.posLocal;
            p1.posModel = p0.posModel;
            p1.overwriteConflict = p0.overwriteConflict;

            FollowArguments fa = p1.followArgs = new FollowArguments();

            fa.followX = p0.followX;
            fa.followY = p0.followY;
            fa.followZ = p0.followZ;
            fa.followYaw = p0.followYaw;
            fa.followPitch = p0.followPitch;

            fa.inheritYaw = p0.inheritYaw;
            fa.inheritPitch = p0.inheritPitch;

            fa.useHead = p0.useHead;
            fa.useRender = p0.useRender;

            return p1;
        }
    }
}
