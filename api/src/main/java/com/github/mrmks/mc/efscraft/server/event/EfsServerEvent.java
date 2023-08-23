package com.github.mrmks.mc.efscraft.server.event;

import com.github.mrmks.mc.efscraft.server.IEfsServerEvent;

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
        private final File keys;

        public Start(S server, List<File> files, File keys) {
            this.server = server;
            this.files = files;
            this.keys = keys;
        }

        public S getServer() {
            return server;
        }

        public List<File> getFiles() {
            return files;
        }

        public File getKeys() {
            return keys;
        }
    }

    enum Stop implements EfsServerEvent {
        INSTANCE;
    }
}
