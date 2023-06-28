package com.github.mrmks.mc.efscraft.common;

import java.util.Arrays;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

class CommandUtils {

    static void doConsumeOptions(EffectEntry entry, String[] options) {
        for (int i = 0; i < options.length; i++) {
            String str = options[i];
            if (str.startsWith("--"))
            {
                int cp = str.charAt(2), bi = 3;

                boolean flag = true;
                if (cp == '!') {
                    flag = false;
                    cp = str.charAt(3);
                    bi = 4;
                }

                switch (cp) {
                    case 'o': entry.overwrite = flag; break;
                    case 'f': {
                        for (int j = bi; j < str.length(); j++) {
                            switch (str.charAt(j)) {
                                case 'x': entry.followX = flag; break;
                                case 'y': entry.followY = flag; break;
                                case 'z': entry.followZ = flag; break;
                                case 'w': entry.followYaw = flag; break;
                                case 'p': entry.followPitch = flag; break;
                            }
                        }
                        break;
                    }
                    case 'u': {
                        for (int j = bi; j < str.length(); j++) {
                            int p = str.charAt(j);
                            if (p == 'h')
                                entry.useHead = flag;
                            else if (p == 'r')
                                entry.useRender = flag;
                        }
                        break;
                    }
                    case 'i': {
                        for (int j = bi; j < str.length(); j++) {
                            int p = str.charAt(j);
                            if (p == 'w')
                                entry.inheritYaw = flag;
                            else if (p == 'p')
                                entry.inheritPitch = flag;
                        }
                        break;
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
                    case "sc": {
                        i += has ? parseXYZ(str, options[i + 1], entry.scale) : fillXYZ(options, i + 1, entry.scale);
                        break;
                    }
                    case "lt": {
                        i += has ? parseXYZ(str, options[i + 1], entry.localPos) : fillXYZ(options, i + 1, entry.localPos);
                        break;
                    }
                    case "mt": {
                        i += has ? parseXYZ(str, options[i + 1], entry.modelPos) : fillXYZ(options, i + 1, entry.modelPos);
                        break;
                    }
                    case "lr": {
                        i += has ? parseWP(str, options[i + 1], entry.localRot) : fillWP(options, i + 1, entry.localRot);
                        break;
                    }
                    case "mr": {
                        i += has ? parseWP(str, options[i + 1], entry.modelRot) : fillWP(options, i + 1, entry.modelRot);
                        break;
                    }
                    case "fs": {
                        entry.skipFrames = parseInt(options[++i]); break;
                    }
                    case "ls": {
                        entry.lifespan = parseInt(options[++i]); break;
                    }
                    case "di": {
                        if (has) {
                            int index = parseInt(str.substring(dot + 1));
                            float value = parseFloat(options[++i]);

                            if (index >= 0) {
                                if (entry.dynamic == null)
                                    entry.dynamic = new float[Math.max(4, index + 1)];
                                else if (entry.dynamic.length <= index)
                                    entry.dynamic = Arrays.copyOf(entry.dynamic, index + 1);

                                entry.dynamic[index] = value;
                            }
                        }
                    }
                }
            }
        }
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
}
