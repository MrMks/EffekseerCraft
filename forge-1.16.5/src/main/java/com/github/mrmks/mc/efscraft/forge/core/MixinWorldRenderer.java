package com.github.mrmks.mc.efscraft.forge.core;

import com.github.mrmks.mc.efscraft.forge.client.RendererImpl;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.common.MinecraftForge;
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
                            value = "FIELD",
                            target = "Lnet/minecraft/client/renderer/WorldRenderer;transparencyChain:Lnet/minecraft/client/shader/ShaderGroup;",
                            shift = At.Shift.BEFORE,
                            ordinal = 0
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
    private void afterRenderParticle(MatrixStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, ActiveRenderInfo pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new RendererImpl.RenderParticleEvent(pPartialTicks, pFinishTimeNano, pMatrixStack.last().pose(), pProjection, pActiveRenderInfo));
    }

}
