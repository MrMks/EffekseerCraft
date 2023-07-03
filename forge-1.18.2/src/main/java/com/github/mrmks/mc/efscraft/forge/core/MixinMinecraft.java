package com.github.mrmks.mc.efscraft.forge.core;

import com.github.mrmks.mc.efscraft.forge.EffekseerCraft;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(
            method = "crash",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/server/ServerLifecycleHooks;handleExit(I)V", remap = false)
    )
    private static void callEfsClean0(CallbackInfo ci) {
        EffekseerCraft.callbackCleanup();
    }

    @Inject(
            method = "destroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;close()V")
    )
    private void callEfsClean1(CallbackInfo ci) {
        EffekseerCraft.callbackCleanup();
    }

}
