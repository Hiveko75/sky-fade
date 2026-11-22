package io.github.hiveko.skyfade.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import io.github.hiveko.skyfade.ShaderPatch;
import io.github.hiveko.skyfade.SkyFade;
import io.github.hiveko.skyfade.SkyTexture;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the sky texture to the terrain shader each time Sodium draws a terrain layer. */
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererMixin {
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0))
    private void skyFade$bindSky(CallbackInfo ci, @Local(argsOnly = true) RenderPass pass) {
        if (SkyFade.shaderPackInUse()) return;
        GpuTextureView sky = SkyTexture.view();
        if (sky != null) {
            pass.setUniform(ShaderPatch.SAMPLER, sky, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }
}
