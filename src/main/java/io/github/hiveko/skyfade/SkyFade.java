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
 */
public class SkyFade implements ClientModInitializer {
    public static final String MOD_ID = "sky-fade";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sky Fade");

    private static final long SHADER_PACK_CHECK_MS = 500;
    private static boolean irisChecked;
    private static Object irisApi;
    private static Method irisPackInUse;
    private static boolean shaderPackInUse;
    private static long shaderPackCheckedAt;

    @Override
    public void onInitializeClient() {
        if (SodiumCheck.matches()) {
            LOGGER.info("Sky Fade active (Sodium {})", SodiumCheck.installed());
        } else {
            LOGGER.warn("Sky Fade off: made for Sodium {}, found {}", SodiumCheck.expected(), SodiumCheck.installed());
        }
    }

    /** True while an Iris shader pack is in use: the shader pack draws the terrain, Sky Fade does nothing. */
    public static boolean shaderPackInUse() {
        long now = System.currentTimeMillis();
        if (now - shaderPackCheckedAt < SHADER_PACK_CHECK_MS) return shaderPackInUse;
        shaderPackCheckedAt = now;
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
        boolean inUse = false;
        if (irisPackInUse != null) {
            try {
                inUse = (Boolean) irisPackInUse.invoke(irisApi);
            } catch (ReflectiveOperationException | RuntimeException e) {
                inUse = false;
            }
        }
        shaderPackInUse = inUse;
        return inUse;
    }
}
