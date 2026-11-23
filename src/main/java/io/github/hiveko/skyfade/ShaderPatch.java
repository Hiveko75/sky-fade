package io.github.hiveko.skyfade;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.hiveko.skyfade.config.SkyFadeConfig;

/**
 * The change to Sodium's terrain fragment shader (blocks/block_layer_opaque.fsh).
 *
 * <p>Sodium ends the fragment colour with one fog step ({@code _linearFog}). Since Minecraft 1.21.6
 * that is the stronger of the normal ("environmental") fog and the render distance fog, mixed towards
 * the fog colour. Here it becomes two steps. First {@code _linearFog} with the render distance fog left
 * out, so the normal fog is what it was, including a {@code fog.glsl} replaced by a resource pack.
 * Then the render distance fog, as a fade from that result towards the sky picture
 * ({@value #SAMPLER}) at this pixel. Before the render distance fog starts, the result is exactly
 * Sodium's.
 *
 * <p>Older versions (Minecraft 1.21.1) have a single fog with one start and end. There the fog keeps
 * its strength and only its colour becomes the sky at this pixel.
 *
 * <p>While the fade is paused, a 1x1 texture is bound instead of the sky; the shader sees that from the
 * texture size and uses Sodium's original line.
 *
 * <p>The fade width setting ({@link SkyFadeConfig#fadeWidth()}, blocks) is written into the shader as a
 * number: the fade then starts that far before the end of the render distance fog instead of where the
 * game's render distance fog starts. With a single fog (1.21.1) it can only start earlier, never later,
 * so water, lava and other close fogs are not weakened.
 */
public final class ShaderPatch {
    /** Name of the sky texture in the shader. */
    public static final String SAMPLER = "sf_Sky";

    private static final String ARG = "\\s*([\\w.]+)\\s*";
    private static final Pattern BLOCK_TEXTURE = Pattern.compile("uniform\\s+sampler2D\\s+u_BlockTex\\s*;[^\\n]*\\n");
    /** "return _linearFog(color, distance, fogColor, environmentFog, renderFog, fade);" or "fragColor = ...". */
    private static final Pattern FOG_TWO = Pattern.compile(
            "([ \\t]*)(return\\s+|fragColor\\s*=\\s*)_linearFog\\(" + ARG + "," + ARG + "," + ARG + "," + ARG + "," + ARG + "," + ARG + "\\)\\s*;");
    /** "fragColor = _linearFog(color, distance, fogColor, fogStart, fogEnd);" (single fog). */
    private static final Pattern FOG_ONE = Pattern.compile(
            "([ \\t]*)(return\\s+|fragColor\\s*=\\s*)_linearFog\\(" + ARG + "," + ARG + "," + ARG + "," + ARG + "," + ARG + "\\)\\s*;");

    private static final String DECLARATION =
            "uniform sampler2D " + SAMPLER + "; // Sky Fade: the sky as drawn this frame, before the terrain\n";

    private static boolean warned;

    private ShaderPatch() {}

    /** True for a source that looks like Sodium's terrain fragment shader. */
    public static boolean isTerrainFragment(String source) {
        return source != null && source.contains("u_BlockTex") && source.contains("_linearFog(")
                && source.contains("fragColor");
    }

    /** The shader source with the sky fade, or the source unchanged when it does not look as expected. */
    public static String apply(String source) {
        if (source == null || source.contains(SAMPLER)) return source;
        Matcher texture = BLOCK_TEXTURE.matcher(source);
        if (!texture.find()) return unexpected(source);

        String fade;
        Matcher fog = FOG_TWO.matcher(source);
        if (fog.find(texture.end())) {
            fade = twoFogs(fog.group(1), fog.group(2).trim().equals("return") ? "return " : "fragColor = ",
                    fog.group(3), fog.group(4), fog.group(5), fog.group(6), fog.group(7), fog.group(8));
        } else {
            fog = FOG_ONE.matcher(source);
            if (!fog.find(texture.end())) return unexpected(source);
            fade = oneFog(fog.group(1), fog.group(2).trim().equals("return") ? "return " : "fragColor = ",
                    fog.group(3), fog.group(4), fog.group(5), fog.group(6), fog.group(7));
        }

        StringBuilder out = new StringBuilder(source.length() + 1536);
        out.append(source, 0, texture.end());
        out.append(DECLARATION);
        out.append(source, texture.end(), fog.start());
        out.append(fade);
        out.append(source, fog.end(), source.length());
        return out.toString();
    }

    private static String twoFogs(String in, String set, String color, String distance, String fogColor,
                                  String environmentFog, String renderFog, String fade) {
        String original = "_linearFog(" + String.join(", ", color, distance, fogColor, environmentFog, renderFog, fade) + ")";
        return String.join("\n",
                in + "if (textureSize(" + SAMPLER + ", 0).x <= 1) {",
                in + "    // Sky Fade paused (a 1x1 texture is bound): Sodium's own fog",
                in + "    " + set + original + ";",
                in + "} else {",
                in + "    // Sky Fade: the normal fog as before (render distance fog left out), then fade from that into the sky",
                in + "    vec4 sf_color = _linearFog(" + String.join(", ", color, distance, fogColor, environmentFog, "vec2(1.0e30, 2.0e30)", fade) + ");",
                "#ifdef USE_FOG",
                in + "    float sf_edge = linear_fog_value(" + distance + ".x, " + start(renderFog + ".x", renderFog + ".y", false) + ", " + renderFog + ".y);",
                in + "    if (sf_edge > 0.0) {",
                in + "        vec3 sf_sky = texelFetch(" + SAMPLER + ", ivec2(gl_FragCoord.xy), 0).rgb;",
                "#ifdef OIT_ACCUMULATE",
                in + "        sf_sky *= " + color + ".a;",
                "#endif",
                in + "        sf_color.rgb = mix(sf_color.rgb, sf_sky, sf_edge * " + fogColor + ".a);",
                in + "    }",
                "#endif",
                in + "    " + set + "sf_color;",
                in + "}");
    }

    private static String oneFog(String in, String set, String color, String distance, String fogColor,
                                 String fogStart, String fogEnd) {
        return String.join("\n",
                in + "// Sky Fade: the fog as before, but towards the sky at this pixel (not while a 1x1 texture is bound)",
                in + "vec4 sf_fog = " + fogColor + ";",
                in + "if (textureSize(" + SAMPLER + ", 0).x > 1) {",
                in + "    sf_fog.rgb = texelFetch(" + SAMPLER + ", ivec2(gl_FragCoord.xy), 0).rgb;",
                in + "}",
                in + set + "_linearFog(" + String.join(", ", color, distance, "sf_fog", start(fogStart, fogEnd, true), fogEnd) + ");");
    }

    /** Where the fade starts: the game's own start, or the set width before the end. */
    private static String start(String gameStart, String end, boolean onlyEarlier) {
        int width = SkyFadeConfig.fadeWidth();
        if (width <= 0) return gameStart;
        String set = "max(0.0, " + end + " - " + width + ".0)";
        return onlyEarlier ? "min(" + gameStart + ", " + set + ")" : set;
    }

    private static String unexpected(String source) {
        if (!warned) {
            warned = true;
            SkyFade.LOGGER.warn("Sodium's terrain shader does not look as expected (a resource pack may replace it); "
                    + "Sky Fade leaves it alone");
        }
        return source;
    }
}
