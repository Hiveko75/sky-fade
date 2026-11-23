package io.github.hiveko.skyfade.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.github.hiveko.skyfade.SkyFade;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Sky Fade's settings, in config/sky-fade.properties.
 *
 * <p>{@code fade_width}: how many blocks before the edge of the render distance the fade into the sky
 * starts. 0 means the game's own render distance fog, which is a tenth of the render distance, between
 * 4 and 64 blocks. The value is built into Sodium's terrain shader, so a change counts after the
 * shaders are made again (a resource reload, done by the settings screen).
 */
public final class SkyFadeConfig {
    public static final int MAX_CHUNKS = 16;
    private static final String FILE = "sky-fade.properties";
    private static int fadeWidth = -1;

    private SkyFadeConfig() {}

    /** The fade width in blocks; 0 for the game's own. */
    public static int fadeWidth() {
        if (fadeWidth < 0) load();
        return fadeWidth;
    }

    public static void setFadeWidth(int blocks) {
        fadeWidth = Math.max(0, Math.min(MAX_CHUNKS * 16, blocks));
        save();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private static void load() {
        fadeWidth = 0;
        Path file = file();
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            fadeWidth = Math.max(0, Math.min(MAX_CHUNKS * 16, Integer.parseInt(props.getProperty("fade_width", "0").trim())));
        } catch (IOException | NumberFormatException e) {
            SkyFade.LOGGER.warn("Could not read {} ({}); using the game's own fade width", file, e.toString());
        }
    }

    private static void save() {
        Path file = file();
        Properties props = new Properties();
        props.setProperty("fade_width", Integer.toString(fadeWidth));
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(writer, "Sky Fade. fade_width: blocks before the edge of the render distance where the fade into the sky starts (0 = the game's own: a tenth of the render distance, 4 to 64 blocks)");
            }
        } catch (IOException e) {
            SkyFade.LOGGER.warn("Could not save {} ({})", file, e.toString());
        }
    }
}
