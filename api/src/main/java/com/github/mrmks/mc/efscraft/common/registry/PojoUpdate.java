package com.github.mrmks.mc.efscraft.common.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PojoUpdate {

    public static <T> Map<String, T> update(Class<T> klass, Gson gson, JsonObject jo, int fromVer) {

        Method method;
        try {
            method = klass.getDeclaredMethod("readFrom", Gson.class, JsonObject.class, int.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return null;
        }
        Map<String, T> map = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            try {
                T ins = (T) method.invoke(null, gson, entry.getValue(), fromVer);

                map.put(entry.getKey(), ins);
            } catch (Throwable tr) {
                tr.printStackTrace();
            }
        }

        return map;
    }

}
