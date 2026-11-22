package io.github.hiveko.skyfade;

import java.lang.reflect.Method;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sky Fade: terrain at the edge of the render distance fades into the sky behind it.
 *
 * <p>How it works: after the sky has been drawn and before the terrain is, the picture of the sky is
 * copied into a texture. Sodium's terrain shader then applies the normal fog first (fog colour and
 * distances exactly as the game or another mod set them) and, across the render distance fog, fades
 * from that result into the copied sky at the same pixel. Without this mod the same stretch fades into
 * one flat fog colour, which shows as a silhouette against a sky of another colour.
 *
 * <p>The idea is the "fog override" of Chunks Fade In by kerudion (GPLv3). That one replaces the fog
 * colour with the sky from the start of the render distance fog on, also for the normal fog that is
 * already there at that distance, so the colour jumps when another mod uses a strong fog colour.
 *
 * <p>The fade is paused ({@link #active()}) while an Iris shader pack is in use and while Voxy is
 * rendering; the terrain shader then does exactly what Sodium's does.
 */
public class SkyFade implements ClientModInitializer {
    public static final String MOD_ID = "sky-fade";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sky Fade");

    private static final long CHECK_MS = 500;
    private static long checkedAt;
    private static boolean active = true;
    private static boolean shaderPack;

    private static boolean irisChecked;
    private static Object irisApi;
    private static Method irisPackInUse;

    private static boolean voxyChecked;
    private static Object voxyConfig;
    private static Method voxyRendering;

    @Override
    public void onInitializeClient() {
        if (SodiumCheck.matches()) {
            LOGGER.info("Sky Fade active (Sodium {}); it pauses while a shader pack is in use or Voxy is rendering",
                    SodiumCheck.installed());
        } else {
            LOGGER.warn("Sky Fade off: made for Sodium {}, found {}", SodiumCheck.expected(), SodiumCheck.installed());
        }
    }

    /**
     * True when the fade should be drawn: not while an Iris shader pack is in use (the shader pack draws
     * the terrain) and not while Voxy is rendering (its distant terrain continues behind the edge of the
     * world, so there is no sky to fade into). Checked again every {@value #CHECK_MS} ms, so switching
     * shaders or Voxy on and off in game is picked up.
     */
    public static boolean active() {
        refresh();
        return active;
    }

    /** True while an Iris shader pack is in use: Sodium's terrain shader is not the one drawing then. */
    public static boolean shaderPack() {
        refresh();
        return shaderPack;
    }

    private static final long STALE_NANOS = 1_000_000_000L;
    private static long skyCopiedAt;

    /** Called each time the sky was copied. */
    public static void skyCopied() {
        skyCopiedAt = System.nanoTime();
    }

    /** True when the fade is on and there is a sky picture from within the last second to fade into. */
    public static boolean skyReady() {
        return active() && skyCopiedAt != 0 && System.nanoTime() - skyCopiedAt < STALE_NANOS;
    }

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - checkedAt >= CHECK_MS) {
            checkedAt = now;
            shaderPack = shaderPackInUse();
            active = !shaderPack && !voxyRendering();
        }
    }

    private static boolean shaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            if (FabricLoader.getInstance().isModLoaded("iris")) {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    irisApi = api.getMethod("getInstance").invoke(null);
                    irisPackInUse = api.getMethod("isShaderPackInUse");
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOGGER.warn("Could not reach Iris; Sky Fade will not notice shader packs", e);
                }
            }
        }
        if (irisPackInUse == null) return false;
        try {
            return (Boolean) irisPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean voxyRendering() {
        if (!voxyChecked) {
            voxyChecked = true;
            if (FabricLoader.getInstance().isModLoaded("voxy")) {
                try {
                    Class<?> config = Class.forName("me.cortex.voxy.client.config.VoxyConfig");
                    voxyConfig = config.getField("CONFIG").get(null);
                    voxyRendering = config.getMethod("isRenderingEnabled");
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOGGER.warn("Could not reach Voxy's settings; Sky Fade stays off while Voxy is installed", e);
                }
            }
        }
        if (!FabricLoader.getInstance().isModLoaded("voxy")) return false;
        if (voxyRendering == null || voxyConfig == null) return true;
        try {
            return (Boolean) voxyRendering.invoke(voxyConfig);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }
}
