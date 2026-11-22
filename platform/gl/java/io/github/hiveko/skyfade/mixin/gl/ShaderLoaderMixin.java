package io.github.hiveko.skyfade.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.hiveko.skyfade.ShaderPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rewrites Sodium's terrain fragment shader as Sodium reads it ({@link ShaderPatch}). These Sodium
 * versions load their shaders themselves; the shader is recognised by its content.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class ShaderLoaderMixin {
    @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
    private static String skyFade$patchTerrainShader(String source) {
        return ShaderPatch.isTerrainFragment(source) ? ShaderPatch.apply(source) : source;
    }
}
