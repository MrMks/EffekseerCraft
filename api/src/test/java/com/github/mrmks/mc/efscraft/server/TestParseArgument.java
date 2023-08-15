package com.github.mrmks.mc.efscraft.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestParseArgument {

    @Test
    public void testParse() {
        String cmd = "-sc 0.1 0.1 0.1 -lt.y 10 -mr.w 30 --!fxyz --fwp --iw --!ip --o -ls 200 -fs 0 -di.0 22 -di.1 11 -di.4 44";
        PacketBuilder builder = new PacketBuilder();

        CommandUtils.doConsumeOptions(builder, cmd.split(" "));

        Assertions.assertArrayEquals(new float[]{ 0.1f, 0.1f, 0.1f }, builder.scale);
        Assertions.assertArrayEquals(new float[] {0, 10, 0}, builder.localPos);
        Assertions.assertArrayEquals(new float[] {30, 0}, builder.modelRot);

        Assertions.assertFalse(builder.followArgs.followX);
        Assertions.assertFalse(builder.followArgs.followY);
        Assertions.assertFalse(builder.followArgs.followZ);
        Assertions.assertTrue(builder.followArgs.followYaw);
        Assertions.assertTrue(builder.followArgs.followPitch);

        Assertions.assertTrue(builder.followArgs.baseOnCurrentYaw);
        Assertions.assertFalse(builder.followArgs.baseOnCurrentPitch);

        Assertions.assertTrue(builder.overwrite);

        Assertions.assertNotNull(builder.dynamic);
        Assertions.assertTrue(builder.dynamic.length > 2);
        Assertions.assertEquals(22, builder.dynamic[0]);
        Assertions.assertEquals(11, builder.dynamic[1]);

        Assertions.assertEquals(5, builder.dynamic.length);
        Assertions.assertEquals(44, builder.dynamic[4]);
    }

}
