import com.github.mrmks.mc.efscraft.ArgumentUtils;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;
import org.junit.jupiter.api.Test;

public class TestParseArgument {

    @Test
    public void testParse() {
        String cmd = "-s 0.1 0.1 0.1 -lt.y 10 -mr.w 30 --!fxyz --fwp --iw --!ip --o -ls 200 -fs 0";
        SPacketPlayWith play = new SPacketPlayWith();

        ArgumentUtils.parse(play, cmd.split(" "));
    }

}
