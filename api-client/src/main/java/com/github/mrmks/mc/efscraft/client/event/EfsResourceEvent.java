package com.github.mrmks.mc.efscraft.client.event;

import com.github.mrmks.mc.efscraft.client.IEfsClientEvent;

public class EfsResourceEvent implements IEfsClientEvent {
    private EfsResourceEvent() {}

    public static class Reload extends EfsResourceEvent {
        public static final Reload EVENT = new Reload();

        private Reload() {}
    }
}
