package com.github.mrmks.mc.efscraft.common;

/**
 * Server side api and only exist in mods.
 */
public abstract class EfscraftAPI<PL> {
    private static final Object[] LOCK = {};
    private static EfscraftAPI<?> API = null;
    protected void register() {
        synchronized (LOCK) {
            if (API == null) API = this;
        }
    }

    public static <PL> EfscraftAPI<PL> api() {
        //noinspection unchecked
        return (EfscraftAPI<PL>) API;
    }

    public static <PL> EfscraftAPI<PL> api(PL ins) {
        return api();
    }

    public static <PL> EfscraftAPI<PL> api(Class<PL> ins) {
        return api();
    }

}
