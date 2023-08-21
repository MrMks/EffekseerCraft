package com.github.mrmks.mc.efscraft.common;

import java.io.IOException;
import java.io.OutputStream;

public interface IEfsNetworkAdaptor<DO extends OutputStream> {

    DO createOutput();
    void closeOutput(DO output) throws IOException;

}
