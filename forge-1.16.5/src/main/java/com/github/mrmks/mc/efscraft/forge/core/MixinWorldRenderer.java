package com.github.mrmks.mc.efscraft.forge.core;

import com.github.mrmks.mc.efscraft.forge.client.ClientEventHooks;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(
            method = "renderLevel",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
                            shift = At.Shift.BEFORE,
                            ordinal = 5
                    ),
//                    @At(
//                            value = "INVOKE",
//                            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
//                            ordinal = 3
//                    ),
//                    @At(
//                            value = "INVOKE",
//                            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
//                            ordinal = 5
//                    )
            }
    )
    private void prevRenderEffect(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        ClientEventHooks.dispatchRenderEvent(pPartialTicks, pFinishTimeNano, pMatrixStack.last().pose(), pProjection, pActiveRenderInfo, true);
    }

    @Inject(
            method = "renderLevel",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
                            shift = At.Shift.AFTER,
                            ordinal = 5
                    )
            }
    )
    private void postRenderEffect(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        ClientEventHooks.dispatchRenderEvent(pPartialTicks, pFinishTimeNano, pMatrixStack.last().pose(), pProjection, pActiveRenderInfo, false);
    }

}
