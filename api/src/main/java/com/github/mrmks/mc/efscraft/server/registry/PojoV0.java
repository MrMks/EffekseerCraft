package com.github.mrmks.mc.efscraft.server.registry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class PojoV0 {
    @SerializedName("extendsFrom") String parent;
    @SerializedName("effect") String effect;
    @SerializedName("lifespan") Integer lifespan;
    @SerializedName("skipFrame") Integer skipFrame;
    @SerializedName("scale") float[] scale;
    @SerializedName("dynamicInput") float[] dynamics = null;
    @SerializedName("rotateLocal") float[] rotateLocal;
    @SerializedName("rotateModel") float[] rotateModel;
    @SerializedName("translateLocal") float[] posLocal;
    @SerializedName("translateModel") float[] posModel;
    @SerializedName("overwriteConflict") Boolean overwriteConflict = null;

    // properties for SPacketPlayWith
    @SerializedName("followX") Boolean followX = null;
    @SerializedName("followY") Boolean followY = null;
    @SerializedName("followZ") Boolean followZ = null;
    @SerializedName("followYaw") Boolean followYaw = null;
    @SerializedName("followPitch") Boolean followPitch = null;
    @SerializedName("useHead") Boolean useHead = null;
    @SerializedName("useRender") Boolean useRender = null;
    @SerializedName("inheritYaw") Boolean inheritYaw = null;
    @SerializedName("inheritPitch") Boolean inheritPitch = null;

    static PojoV0 readFrom(Gson gson, JsonObject jo, int ver) {
        return gson.fromJson(jo, PojoV0.class);
    }
}
