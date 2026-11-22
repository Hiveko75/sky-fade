package io.github.hiveko.skyfade.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import io.github.hiveko.skyfade.SkyFade;
import io.github.hiveko.skyfade.SkyGl;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a step to the frame between the sky and the terrain: at that point the main colour buffer holds
 * the finished sky (with everything other mods drew into it) and nothing else, and it is copied.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V"))
    private void skyFade$copySky(CallbackInfo ci, @Local FrameGraphBuilder frame) {
        if (!SkyFade.active()) return;
        FramePass pass = frame.addPass("sky_fade_copy_sky");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        ResourceHandle<RenderTarget> main = this.targets.main;
        pass.executes(() -> {
            RenderTarget target = main.get();
            if (target.getColorTexture() instanceof GlTexture color) {
                SkyGl.copyFromTexture(color.glId(), target.width, target.height);
            }
        });
    }
}
