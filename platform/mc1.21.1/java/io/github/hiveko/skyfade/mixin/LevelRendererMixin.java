package io.github.hiveko.skyfade.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.hiveko.skyfade.SkyFade;
import io.github.hiveko.skyfade.SkyGl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right after the sky is drawn and before the terrain: the main framebuffer holds the finished sky
 * (with everything other mods drew into it) and nothing else, and its colour is copied.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
                    shift = At.Shift.AFTER))
    private void skyFade$copySky(CallbackInfo ci) {
        if (!SkyFade.active()) return;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        SkyGl.copyFromFramebuffer(main.frameBufferId, main.width, main.height);
    }
}
