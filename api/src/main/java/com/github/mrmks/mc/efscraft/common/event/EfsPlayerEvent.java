package com.github.mrmks.mc.efscraft.common.event;

import com.github.mrmks.mc.efscraft.common.IEfsServerEvent;

import java.util.UUID;

public abstract class EfsPlayerEvent implements IEfsServerEvent {
    private final UUID player;
    private EfsPlayerEvent(UUID uuid) {
        this.player = uuid;
    }

    public UUID getPlayer() {
        return player;
    }

    public static class Join extends EfsPlayerEvent {
        public Join(UUID uuid) {
            super(uuid);
        }
    }

    public static class Leave extends EfsPlayerEvent {
        public Leave(UUID uuid) {
            super(uuid);
        }
    }

    public static class Verify extends EfsPlayerEvent {
        private final int version;
        public Verify(UUID uuid, int v) {
            super(uuid);
            this.version = v;
        }

        public int getVersion() {
            return version;
        }
    }
}
