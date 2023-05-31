package com.github.mrmks.mc.efscraft.forge.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.*;

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
                "com.github.mrmks.mc.efscraft.forge.core.LoadingPlugin$Transformer",
                "com.github.mrmks.mc.efscraft.forge.core.LoadingPlugin$EntityRendererTransformer",
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

    private static final String EVENT_HOOK = "com/github/mrmks/mc/efscraft/forge/client/ClientEventHooks";

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
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, EVENT_HOOK, "callbackCleanup", "()V", false);
                                }
                                super.visitMethodInsn(opcode, owner, name, desc, itf);
                            }
                        };
                    } else if ((deobf ? "displayCrashReport" : "func_71377_b").equals(name)) {
                        mv = new MethodVisitor(api, mv) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                if ("net/minecraftforge/fml/common/FMLCommonHandler".equals(owner) && "instance".equals(name)) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, EVENT_HOOK, "callbackCleanup", "()V", false);
                                }
                                super.visitMethodInsn(opcode, owner, name, desc, itf);
                            }
                        };
                    } else if ((deobf ? "updateFramebufferSize" : "").equals(name)) {
                        mv = new MethodVisitor(api, mv) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, EVENT_HOOK, "resizeFramebuffers", "()V", false);
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

    public static class EntityRendererTransformer implements IClassTransformer {

        @Override
        public byte[] transform(String name, String transformedName, byte[] basicClass) {
            if (!"net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) return basicClass;

            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(0);

            ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    boolean deobf = FMLLaunchHandler.isDeobfuscatedEnvironment();

                    if ((deobf ? "renderWorldPass" : "func_175068_a").equals(name)) {
                        mv = new MethodVisitor(api, mv) {

                            boolean foundAnchor = false, handled = false;

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                                super.visitMethodInsn(opcode, owner, name, desc, itf);

                                if (!handled && foundAnchor
                                        && "net/minecraft/client/renderer/RenderGlobal".equals(owner)
                                        && (deobf ? "renderBlockLayer" : "").equals(name)) {

                                    handled = true;
                                    genDispatch(false);
                                }
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                                if (!foundAnchor
                                        && opcode == Opcodes.GETSTATIC
                                        && "net/minecraft/util/BlockRenderLayer".equals(owner)
                                        && (deobf ? "TRANSLUCENT" : "").equals(name)) {

                                    foundAnchor = true;
                                    genDispatch(true);
                                }

                                super.visitFieldInsn(opcode, owner, name, desc);
                            }

                            private void genDispatch(boolean prev) {
                                super.visitVarInsn(Opcodes.ILOAD, 1);
                                super.visitVarInsn(Opcodes.FLOAD, 2);
                                super.visitVarInsn(Opcodes.LLOAD, 3);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, EVENT_HOOK, "dispatchRendererEvent" + (prev ? "Prev" : "Post"), "(IFJ)V", false);
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
