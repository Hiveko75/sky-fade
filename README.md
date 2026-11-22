# Sky Fade

Terrain at the edge of the render distance fades into the sky that is actually behind it, instead of
into one flat fog colour.

- The normal fog is applied first, exactly as the game or another mod (for example Atmospherics) set
  its colour and distance.
- Across the render distance fog, the result then fades into the sky at the same place on screen:
  sky colour, sunset, haze, stars, whatever was drawn there.
- Closer than the render distance fog, nothing changes.
- It pauses while an Iris shader pack is in use and while Voxy is rendering (Voxy's distant terrain
  continues behind the edge, so there is no sky to fade into).  Switching either off in game turns
  the fade back on.

Client-side Fabric mod for [Sodium](https://modrinth.com/mod/sodium).

## Supported versions

| Minecraft | Sodium |
|---|---|
| 26.3 | 0.9.3-beta.1 |

The mod hooks into Sodium's terrain renderer and shader, which change between Sodium versions. With a
Sodium version a jar was not made for, the mod stays off and writes one line to the log.

## Credit

The idea and the approach come from the "fog override" option of
[Chunks Fade In](https://github.com/kerudion/chunksfadein) by kerudion (GPLv3). Chunks Fade In swaps
the fog colour for the sky from the start of the render distance fog; with a strong fog colour from
another mod that shows as a line where the swap starts. Sky Fade keeps the normal fog and fades from
it into the sky, and does only that.

## Building

`./gradlew build -Pmc=26.3` (JDK 25). The jar is in `build/libs`.

## License

GPL-3.0-only, see [LICENSE](LICENSE).
