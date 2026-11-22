package io.github.hiveko.skyfade;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Sky Fade hooks into Sodium's terrain renderer and its terrain shader, which change between Sodium
 * versions. Each jar lists the Sodium versions it was made for ({@value #FILE}); with any other
 * version nothing is hooked and the game runs as if the mod were not installed.
 */
public final class SodiumCheck {
    static final String FILE = "sky-fade.versions.properties";
    private static String expected;

    private SodiumCheck() {}

    /** The Sodium versions this jar was made for, as listed ("0.9.3-beta.1" or "0.9.1, 0.9.2"). */
    public static synchronized String expected() {
        if (expected == null) {
            Properties props = new Properties();
            try (InputStream in = SodiumCheck.class.getClassLoader().getResourceAsStream(FILE)) {
                if (in != null) props.load(in);
            } catch (IOException e) {
                // no list: stays off
            }
            expected = props.getProperty("sodium", "").trim();
        }
        return expected;
    }

    /** The installed Sodium version, or "not installed". */
    public static String installed() {
        return FabricLoader.getInstance().getModContainer("sodium")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("not installed");
    }

    /** True when the installed Sodium is one of the listed versions ("0.9.3-beta.1" matches "0.9.3-beta.1+mc26.3"). */
    public static boolean matches() {
        String found = installed();
        int plus = found.indexOf('+');
        if (plus >= 0) found = found.substring(0, plus);
        for (String version : expected().split(",")) {
            if (!version.isBlank() && found.equals(version.trim())) return true;
        }
        return false;
    }
}
