import com.github.mrmks.mc.efscraft.ArgumentUtils;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestParseArgument {

    @Test
    public void testParse() {
        String cmd = "-s 0.1 0.1 0.1 -lt.y 10 -mr.w 30 --!fxyz --fwp --iw --!ip --o -ls 200 -fs 0";
        SPacketPlayWith play = new SPacketPlayWith();

        ArgumentUtils.parse(play, cmd.split(" "));

        Assertions.assertArrayEquals(play.getScale(), new float[]{ 0.1f, 0.1f, 0.1f });
        Assertions.assertArrayEquals(play.getLocalPosition(), new float[] {0, 10, 0});
        Assertions.assertArrayEquals(play.getModelRotation(), new float[] {30, 0});

        Assertions.assertFalse(play.followX());
        Assertions.assertFalse(play.followY());
        Assertions.assertFalse(play.followZ());
        Assertions.assertTrue(play.followYaw());
        Assertions.assertTrue(play.followPitch());

        Assertions.assertTrue(play.isInheritYaw());
        Assertions.assertFalse(play.isInheritPitch());

        Assertions.assertTrue(play.conflictOverwrite());
    }

}
