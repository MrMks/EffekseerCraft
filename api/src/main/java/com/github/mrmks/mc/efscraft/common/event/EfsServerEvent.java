package com.github.mrmks.mc.efscraft.common.event;

import com.github.mrmks.mc.efscraft.common.IEfsServerEvent;

import java.io.File;
import java.util.List;

public interface EfsServerEvent extends IEfsServerEvent {
    class Tick<S> implements EfsServerEvent {

        private final S server;

        public Tick(S server) {
            this.server = server;
        }

        public S getServer() {
            return server;
        }
    }

    class Start<S> implements EfsServerEvent {

        private final S server;
        private final List<File> files;

        public Start(S server, List<File> files) {
            this.server = server;
            this.files = files;
        }

        public S getServer() {
            return server;
        }

        public List<File> getFiles() {
            return files;
        }
    }

    enum Stop implements EfsServerEvent {
        INSTANCE;
    }
}
