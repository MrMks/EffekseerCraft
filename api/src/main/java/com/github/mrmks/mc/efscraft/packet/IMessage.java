package com.github.mrmks.mc.efscraft.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IMessage {
    default void read(DataInput stream) throws IOException {}
    default void write(DataOutput stream) throws IOException {}
}
