package io.github.hiveko.skyfade;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * The copy of the sky for the Sodium versions that draw terrain with their own OpenGL code (Sodium
 * 0.8.x, and 0.9.x on Minecraft 26.1): a plain OpenGL texture the size of the game's main colour
 * buffer, filled once per frame right after the sky was drawn and read by Sodium's terrain shader on
 * texture unit {@value #UNIT}. Everything here is direct OpenGL, and every piece of OpenGL state that
 * is changed is put back, so the game's own bookkeeping of that state stays right.
 */
public final class SkyGl {
    /** Texture unit of the sky texture: above the units the game and Sodium use. */
    public static final int UNIT = 13;

    private static int texture;
    private static int width;
    private static int height;
    private static int pausedTexture;
    private static int readFramebuffer;
    private static int drawFramebuffer;
    private static boolean failed;

    private SkyGl() {}

    /** Copies a colour texture (the main colour buffer, which holds only the sky at this point of the frame). */
    public static void copyFromTexture(int sourceTexture, int sourceWidth, int sourceHeight) {
        if (sourceTexture <= 0) return;
        copy(sourceTexture, 0, sourceWidth, sourceHeight);
    }

    /** Copies the colour of a framebuffer (the main one, which holds only the sky at this point of the frame). */
    public static void copyFromFramebuffer(int sourceFramebuffer, int sourceWidth, int sourceHeight) {
        if (sourceFramebuffer <= 0) return;
        copy(0, sourceFramebuffer, sourceWidth, sourceHeight);
    }

    private static void copy(int sourceTexture, int sourceFramebuffer, int sourceWidth, int sourceHeight) {
        if (failed || sourceWidth <= 0 || sourceHeight <= 0) return;
        try {
            ensure(sourceWidth, sourceHeight);
            int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            if (scissor) GL11.glDisable(GL11.GL_SCISSOR_TEST);

            if (sourceTexture != 0) {
                if (readFramebuffer == 0) readFramebuffer = GL30.glGenFramebuffers();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sourceTexture, 0);
            } else {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFramebuffer);
            }
            if (drawFramebuffer == 0) drawFramebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);

            GL30.glBlitFramebuffer(0, 0, sourceWidth, sourceHeight, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            SkyFade.skyCopied();
        } catch (RuntimeException e) {
            failed = true;
            SkyFade.LOGGER.error("Could not copy the sky for Sky Fade; it is off for this session", e);
        }
    }

    /**
     * Called when Sodium has set up its terrain shader: if that shader has the sky texture (it does unless
     * the shader could not be changed), points it at unit {@value #UNIT} and puts the sky there, or the
     * 1x1 texture while the fade is paused or there is no fresh sky picture.
     */
    public static void bind() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int location = GL20.glGetUniformLocation(program, ShaderPatch.SAMPLER);
        if (location < 0) return;
        int bound = !failed && texture != 0 && SkyFade.skyReady() ? texture : paused();
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, bound);
        GL13.glActiveTexture(previousUnit);
        GL20.glUniform1i(location, UNIT);
    }

    private static void ensure(int wantedWidth, int wantedHeight) {
        if (texture != 0 && width == wantedWidth && height == wantedHeight) return;
        if (texture != 0) GL11.glDeleteTextures(texture);
        texture = create(wantedWidth, wantedHeight);
        width = wantedWidth;
        height = wantedHeight;
    }

    private static int paused() {
        if (pausedTexture == 0) pausedTexture = create(1, 1);
        return pausedTexture;
    }

    /** A new RGBA texture without mipmaps, made on unit {@value #UNIT} so no other unit's binding changes. */
    private static int create(int textureWidth, int textureHeight) {
        int id = GL11.glGenTextures();
        int previousUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812F);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812F);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 0x813D, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, textureWidth, textureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(previousUnit);
        return id;
    }
}
