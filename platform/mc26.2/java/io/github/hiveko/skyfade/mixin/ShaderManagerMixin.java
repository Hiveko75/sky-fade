package io.github.hiveko.skyfade.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import io.github.hiveko.skyfade.ShaderPatch;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Rewrites Sodium's terrain fragment shader as it is handed out for compiling ({@link ShaderPatch}). */
@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @ModifyReturnValue(method = "getShader", at = @At("RETURN"))
    private String skyFade$patchTerrainShader(String source, @Local(argsOnly = true) Identifier id, @Local(argsOnly = true) ShaderType type) {
        if (source == null || type != ShaderType.FRAGMENT || !"sodium".equals(id.getNamespace())) return source;
        String path = id.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.startsWith("block_layer_opaque") ? ShaderPatch.apply(source) : source;
    }
}
