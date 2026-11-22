package io.github.hiveko.skyfade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import io.github.hiveko.skyfade.ShaderPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds the sky texture to the textures Sodium's terrain shader can use (its first bind group, with the block atlas). */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false)
public abstract class ShaderChunkRendererMixin {
    @WrapOperation(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout$Builder;build()Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout;",
                    ordinal = 0))
    private static BindGroupLayout skyFade$addSkyTexture(BindGroupLayout.Builder builder, Operation<BindGroupLayout> original) {
        return original.call(builder.withUniform(ShaderPatch.SAMPLER, UniformType.COMBINED_IMAGE_SAMPLER));
    }
}
