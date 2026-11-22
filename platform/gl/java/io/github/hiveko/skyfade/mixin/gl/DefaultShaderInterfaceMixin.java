package io.github.hiveko.skyfade.mixin.gl;

import io.github.hiveko.skyfade.SkyGl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the sky texture to Sodium's terrain shader each time Sodium sets that shader up for a terrain layer. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap = false)
public abstract class DefaultShaderInterfaceMixin {
    @Inject(method = "setupState", at = @At("TAIL"))
    private void skyFade$bindSky(CallbackInfo ci) {
        SkyGl.bind();
    }
}
