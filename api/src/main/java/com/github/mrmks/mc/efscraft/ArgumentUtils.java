package com.github.mrmks.mc.efscraft;

import com.github.mrmks.mc.efscraft.packet.SPacketPlayAbstract;
import com.github.mrmks.mc.efscraft.packet.SPacketPlayWith;

public class ArgumentUtils {

    private interface Parser {
        int consume(String leader, String[] args, int cur, float[] ary);
    }

    public static void parse(SPacketPlayAbstract packet, String[] args) {

        float[] scale = packet.getScale();
        float[] localTrans = packet.getLocalPosition();
        float[] localRot = packet.getLocalRotation();
        float[] modelTrans = packet.getModelPosition();
        float[] modelRot = packet.getModelRotation();

        for (int i = 0; i < args.length; i++) {
            String str = args[i];
            if (str.startsWith("--"))
            {
                int cp = str.charAt(2), bi = 3;

                boolean flag = true;
                if (cp == '!') {
                    flag = false;
                    cp = str.charAt(3);
                    bi = 4;
                }

                if (cp == 'o') {
                    packet.markConflictOverwrite(flag);
                } else if (packet instanceof SPacketPlayWith) {
                    SPacketPlayWith play = (SPacketPlayWith) packet;

                    switch (cp) {
                        case 'f': {
                            for (int j = bi; j < str.length(); j++) {
                                switch (str.charAt(j)) {
                                    case 'x':
                                        play.markFollowX(flag);
                                        break;
                                    case 'y':
                                        play.markFollowY(flag);
                                        break;
                                    case 'z':
                                        play.markFollowZ(flag);
                                        break;
                                    case 'w':
                                        play.markFollowYaw(flag);
                                        break;
                                    case 'p':
                                        play.markFollowPitch(flag);
                                        break;
                                }
                            }
                            break;
                        }
                        case 'i': {
                            for (int j = bi; j < str.length(); j++) {
                                switch (str.charAt(j)) {
                                    case 'w':
                                        play.markInheritYaw(flag);
                                        break;
                                    case 'p':
                                        play.markInheritPitch(flag);
                                        break;
                                }
                            }
                            break;
                        }
                        case 'u': {
                            for (int j = bi; j < str.length(); j++) {
                                switch (str.charAt(j)) {
                                    case 'h':
                                        play.markUseHead(flag);
                                        break;
                                    case 'r':
                                        play.markUseRender(flag);
                                        break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            else if (str.charAt(0) == '-')
            {
                int lastDot = str.lastIndexOf('.'), dot = str.indexOf('.');

                if (lastDot != dot) continue;
                if (!(dot == str.length() - 2 || dot < 0)) continue; // non '.' or '.' isn't the last two char.

                boolean has = dot > 0;
                String sub = has ? str.substring(1, dot) : str.substring(1);

                switch (sub) {
                    case "s": {
                        i += has ? parseXYZ(str, args[i + 1], scale) : fillXYZ(args, i + 1, scale);
                        break;
                    }
                    case "lt": {
                        i += has ? parseXYZ(str, args[i + 1], localTrans) : fillXYZ(args, i + 1, localTrans);
                        break;
                    }
                    case "mt": {
                        i += has ? parseXYZ(str, args[i + 1], modelTrans) : fillXYZ(args, i + 1, modelTrans);
                        break;
                    }
                    case "lr": {
                        i += has ? parseWP(str, args[i + 1], localRot) : fillWP(args, i + 1, localRot);
                        break;
                    }
                    case "mr": {
                        i += has ? parseWP(str, args[i + 1], modelRot) : fillWP(args, i + 1, modelRot);
                        break;
                    }
                    case "fs": {
                        packet.skipFrame(parseInt(args[++i]));
                        break;
                    }
                    case "ls": {
                        packet.setLifespan(parseInt(args[++i]));
                        break;
                    }
                    case "di": {
                        if (has) {
                            int index = parseInt(str.substring(dot + 1));
                            float value = parseFloat(args[++i]);

                            packet.setDynamic(index, value);
                        }
                    }
                }
            }
        }

        packet.scaleTo(scale[0], scale[1], scale[2]);
        packet.translateLocalTo(localTrans[0], localTrans[1], localTrans[2]);
        packet.translateModelTo(modelTrans[0], modelTrans[1], modelTrans[2]);
        packet.rotateLocalTo(localRot[0], localRot[1]);
        packet.rotateModelTo(modelRot[0], modelRot[1]);
    }

    private static int parseXYZ(String str, String p, float[] ary) {
        int code = str.charAt(str.length() - 1);
        float value = parseFloat(p);
        if (code == 'x') {
            ary[0] = value;
        } else if (code == 'y') {
            ary[1] = value;
        } else if (code == 'z') {
            ary[2] = value;
        }
        return 1;
    }

    private static int fillXYZ(String[] arg, int bi, float[] ary) {
        for (int i = 0; i < 3; i++)
            ary[i] = parseFloat(arg[bi + i]);
        return 3;
    }

    private static int parseWP(String str, String p, float[] ary) {
        int code = str.charAt(str.length() - 1);
        float value = parseFloat(p);
        if (code == 'w') {
            ary[0] = value;
        } else if (code == 'p') {
            ary[1] = value;
        }
        return 1;
    }

    private static int fillWP(String[] arg, int bi, float[] ary) {
        for (int i = 0; i < 2; i++)
            ary[i] = parseFloat(arg[bi + i]);
        return 2;
    }

    private static int parseInt(String p) {
        return Integer.parseInt(p);
    }

    private static float parseFloat(String p) {
        return Float.parseFloat(p);
    }
}
