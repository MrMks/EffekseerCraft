package com.github.mrmks.mc.efscraft.forge.core;

import com.github.mrmks.mc.efscraft.forge.client.ClientProxy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow private int ticks;

    @Shadow @Nullable private Frustum capturedFrustum;

    @Shadow private Frustum cullingFrustum;

    @Inject(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lnet/minecraft/client/Camera;)V", shift = At.Shift.BEFORE)
    )
    public void renderBeforeDebug(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        ForgeHooksClient.dispatchRenderStage(ClientProxy.BEFORE_DEBUG, (LevelRenderer) (Object) this, pPoseStack, pProjectionMatrix, ticks, pCamera, this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    public void renderBeforeTransparency(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        ForgeHooksClient.dispatchRenderStage(ClientProxy.BEFORE_TRANSPARENCY, (LevelRenderer) (Object) this, pPoseStack, pProjectionMatrix, ticks, pCamera, this.capturedFrustum == null ? this.cullingFrustum : this.capturedFrustum);
    }
}
