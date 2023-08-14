package com.github.mrmks.mc.efscraft.common.event;

import com.github.mrmks.mc.efscraft.common.IEfsServerEvent;

public class EfsTickEvent implements IEfsServerEvent {
    public static final EfsTickEvent INSTANCE = new EfsTickEvent();

    private EfsTickEvent() {}
}
