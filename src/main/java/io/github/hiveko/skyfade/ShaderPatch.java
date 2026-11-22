package io.github.hiveko.skyfade;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The change to Sodium's terrain fragment shader (blocks/block_layer_opaque.fsh).
 *
 * <p>Sodium ends the fragment colour with one fog step: the stronger of the normal ("environmental")
 * fog and the render distance fog, mixed towards the fog colour. Here that becomes two steps: first
 * the normal fog towards the fog colour, unchanged; then the render distance fog as a fade from that
 * result towards the sky picture ({@value #SAMPLER}) at this pixel. Before the render distance fog
 * starts, the result is exactly Sodium's.
 */
public final class ShaderPatch {
    /** Name of the sky texture in the shader and in Sodium's terrain bind group. */
    public static final String SAMPLER = "sf_Sky";

    private static final Pattern BLOCK_TEXTURE = Pattern.compile("uniform\\s+sampler2D\\s+u_BlockTex\\s*;[^\\n]*\\n");
    private static final Pattern FOG_RETURN = Pattern.compile(
            "([ \\t]*)return\\s+_linearFog\\(\\s*color\\s*,\\s*v_FragDistance\\s*,\\s*fogColor\\s*,\\s*u_EnvironmentFog\\s*,"
                    + "\\s*u_RenderFog\\s*,\\s*factor\\s*\\)\\s*;");

    private static final String DECLARATION =
            "uniform sampler2D " + SAMPLER + "; // Sky Fade: the sky as drawn this frame, before the terrain\n";

    private static final String FADE = String.join("\n",
            "#ifdef USE_FOG",
            "    // Sky Fade: the normal fog first, then fade from that into the sky across the render distance fog",
            "    float sf_fog = max(1.0 - factor, linear_fog_value(v_FragDistance.y, u_EnvironmentFog.x, u_EnvironmentFog.y));",
            "    float sf_edge = linear_fog_value(v_FragDistance.x, u_RenderFog.x, u_RenderFog.y);",
            "    vec3 sf_rgb = mix(color.rgb, fogColor.rgb, sf_fog * fogColor.a);",
            "    if (sf_edge > 0.0) {",
            "        vec3 sf_sky = texelFetch(" + SAMPLER + ", ivec2(gl_FragCoord.xy), 0).rgb;",
            "    #ifdef OIT_ACCUMULATE",
            "        sf_sky *= color.a;",
            "    #endif",
            "        sf_rgb = mix(sf_rgb, sf_sky, sf_edge * fogColor.a);",
            "    }",
            "    return vec4(sf_rgb, color.a);",
            "#else",
            "    return _linearFog(color, v_FragDistance, fogColor, u_EnvironmentFog, u_RenderFog, factor);",
            "#endif");

    private static boolean warned;

    private ShaderPatch() {}

    /** The shader source with the sky fade, or the source unchanged when it does not look as expected. */
    public static String apply(String source) {
        if (source == null || source.contains(SAMPLER)) return source;
        Matcher texture = BLOCK_TEXTURE.matcher(source);
        Matcher fog = FOG_RETURN.matcher(source);
        if (!texture.find() || !fog.find() || fog.start() < texture.end()) {
            if (!warned) {
                warned = true;
                SkyFade.LOGGER.warn("Sodium's terrain shader does not look as expected (a resource pack may replace it); "
                        + "Sky Fade leaves it alone");
            }
            return source;
        }
        StringBuilder out = new StringBuilder(source.length() + 1024);
        out.append(source, 0, texture.end());
        out.append(DECLARATION);
        out.append(source, texture.end(), fog.start());
        out.append(FADE);
        out.append(source, fog.end(), source.length());
        return out.toString();
    }
}
