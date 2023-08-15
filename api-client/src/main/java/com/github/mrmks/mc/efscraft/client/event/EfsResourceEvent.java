package com.github.mrmks.mc.efscraft.client.event;

import com.github.mrmks.mc.efscraft.client.IEfsClientEvent;

public interface EfsResourceEvent extends IEfsClientEvent {
    enum Reload implements EfsResourceEvent {
        INSTANCE
    }
}
