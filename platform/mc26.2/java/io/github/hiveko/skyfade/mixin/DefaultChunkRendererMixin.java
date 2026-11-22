package io.github.hiveko.skyfade.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.hiveko.skyfade.ShaderPatch;
import io.github.hiveko.skyfade.SkyFade;
import io.github.hiveko.skyfade.SkyTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the sky texture (or, while the fade is paused, a 1x1 texture) to the terrain shader for each terrain layer. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer", remap = false)
public abstract class DefaultChunkRendererMixin {
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture(Ljava/lang/String;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0))
    private void skyFade$bindSky(CallbackInfo ci, @Local RenderPass pass) {
        if (SkyFade.shaderPack()) return;
        GpuTextureView sky = SkyFade.skyReady() ? SkyTexture.view() : null;
        if (sky == null) sky = SkyTexture.paused();
        if (sky != null) {
            pass.bindTexture(ShaderPatch.SAMPLER, sky, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }
}
