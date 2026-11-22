package io.github.hiveko.skyfade;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

/**
 * The copy of the sky: a texture with the size and format of the game's main colour buffer, filled
 * once per frame right after the sky was drawn ({@link #copyFrom}) and read by Sodium's terrain shader.
 */
public final class SkyTexture {
    private static TextureTarget target;
    private static GpuFormat format;
    private static TextureTarget pausedTarget;

    private SkyTexture() {}

    /** Copies the colour buffer (which holds only the sky at this point of the frame) into the sky texture. */
    public static void copyFrom(GpuTexture source) {
        if (source == null || source.isClosed()) return;
        try {
            GpuTexture sky = ensure(source.getWidth(0), source.getHeight(0), source.getFormat());
            if (sky == null) return;
            RenderSystem.getDevice().createCommandEncoder()
                    .copyTextureToTexture(source, sky, 0, 0, 0, 0, 0, source.getWidth(0), source.getHeight(0));
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /** The sky texture for the terrain shader; created with the main buffer's size if there is none yet. */
    public static GpuTextureView view() {
        if (target == null) {
            RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            GpuTexture color = main == null ? null : main.getColorTexture();
            if (color == null) return null;
            try {
                ensure(color.getWidth(0), color.getHeight(0), color.getFormat());
            } catch (RuntimeException e) {
                fail(e);
            }
        }
        return target == null ? null : target.getColorTextureView();
    }

    /** A 1x1 texture: bound while the fade is paused, which the shader recognises by its size. */
    public static GpuTextureView paused() {
        if (pausedTarget == null) {
            try {
                pausedTarget = new TextureTarget("Sky Fade paused", 1, 1, GpuFormat.RGBA8_UNORM, null);
            } catch (RuntimeException e) {
                fail(e);
                return null;
            }
        }
        return pausedTarget.getColorTextureView();
    }

    private static GpuTexture ensure(int width, int height, GpuFormat wanted) {
        if (width <= 0 || height <= 0) return null;
        if (target != null && (target.width != width || target.height != height || format != wanted)) {
            target.destroyBuffers();
            target = null;
        }
        if (target == null) {
            target = new TextureTarget("Sky Fade sky copy", width, height, wanted, null);
            format = wanted;
        }
        return target.getColorTexture();
    }

    private static boolean failed;

    private static void fail(RuntimeException e) {
        if (!failed) {
            failed = true;
            SkyFade.LOGGER.error("Could not copy the sky for Sky Fade; the edge of the world may show a wrong colour", e);
        }
    }
}
