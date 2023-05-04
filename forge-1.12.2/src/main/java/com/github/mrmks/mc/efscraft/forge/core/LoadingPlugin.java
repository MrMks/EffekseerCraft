package com.github.mrmks.mc.efscraft.forge.core;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.objectweb.asm.Opcodes;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("efscraft_inject")
@IFMLLoadingPlugin.TransformerExclusions("com.gituhb.mrmks.mc.efscraft.forge.core")
@IFMLLoadingPlugin.SortingIndex(1001)
public class LoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                "com.github.mrmks.mc.efscraft.forge.core.LoadingPlugin$Transformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static class Transformer implements IClassTransformer {
        @Override
        public byte[] transform(String name, String transformedName, byte[] basicClass) {
            if (!"net.minecraft.client.Minecraft".equals(transformedName)) return basicClass;

            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(0);

            ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    boolean deobf = FMLLaunchHandler.isDeobfuscatedEnvironment();

                    if ((deobf ? "shutdownMinecraftApplet" : "func_71405_e").equals(name)) {
                        mv = new MethodVisitor(api, mv) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                if ("org/lwjgl/opengl/Display".equals(owner) && "destroy".equals(name)) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/github/mrmks/mc/efscraft/forge/EffekseerCraft", "callbackCleanup", "()V", false);
                                }
                                super.visitMethodInsn(opcode, owner, name, desc, itf);
                            }
                        };
                    } else if ((deobf ? "displayCrashReport" : "func_71377_b").equals(name)) {
                        mv = new MethodVisitor(api, mv) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                if ("net/minecraftforge/fml/common/FMLCommonHandler".equals(owner) && "instance".equals(name)) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/github/mrmks/mc/efscraft/forge/EffekseerCraft", "callbackCleanup", "()V", false);
                                }
                                super.visitMethodInsn(opcode, owner, name, desc, itf);
                            }
                        };
                    }
                    return mv;
                }
            };

            cr.accept(cv, 0);

            return cw.toByteArray();
        }
    }
}
