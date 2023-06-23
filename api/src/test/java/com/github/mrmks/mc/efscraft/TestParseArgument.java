package com.github.mrmks.mc.efscraft;

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

        Assertions.assertFalse(builder.followX);
        Assertions.assertFalse(builder.followY);
        Assertions.assertFalse(builder.followZ);
        Assertions.assertTrue(builder.followYaw);
        Assertions.assertTrue(builder.followPitch);

        Assertions.assertTrue(builder.inheritYaw);
        Assertions.assertFalse(builder.inheritPitch);

        Assertions.assertTrue(builder.overwrite);

        Assertions.assertNotNull(builder.dynamic);
        Assertions.assertTrue(builder.dynamic.length > 2);
        Assertions.assertEquals(22, builder.dynamic[0]);
        Assertions.assertEquals(11, builder.dynamic[1]);

        Assertions.assertEquals(5, builder.dynamic.length);
        Assertions.assertEquals(44, builder.dynamic[4]);
    }

}
