package com.github.mrmks.mc.efscraft.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IEfsNetworkAdaptor<DI extends DataInput, DO extends DataOutput> {

    DO createOutput();
    void closeOutput(DO output) throws IOException;

}
