package com.github.mrmks.mc.efscraft.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestParseArgument {

    @Test
    public void testParse() {
        String cmd = "-sc 0.1 0.1 0.1 -lt.y 10 -mr.w 30 --!fxyz --fwp --iw --!ip --o -ls 200 -fs 0";
        PacketBuilder builder = new PacketBuilder();

        CommandUtils.doConsumeOptions(builder, cmd.split(" "));

        Assertions.assertArrayEquals(builder.scale, new float[]{ 0.1f, 0.1f, 0.1f });
        Assertions.assertArrayEquals(builder.localPos, new float[] {0, 10, 0});
        Assertions.assertArrayEquals(builder.modelRot, new float[] {30, 0});

        Assertions.assertFalse(builder.followX);
        Assertions.assertFalse(builder.followY);
        Assertions.assertFalse(builder.followZ);
        Assertions.assertTrue(builder.followYaw);
        Assertions.assertTrue(builder.followPitch);

        Assertions.assertTrue(builder.inheritYaw);
        Assertions.assertFalse(builder.inheritPitch);

        Assertions.assertTrue(builder.overwrite);
    }

}
