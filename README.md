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

## Settings

**Fade width** (Mod Menu → Sky Fade → Configure, or `fade_width` in `config/sky-fade.properties`): how
far before the edge of the render distance the fade into the sky starts, from 1 to 16 chunks, or
**Game default** (a tenth of the render distance, 4 to 64 blocks: the game's own render distance fog).
A wider fade hides the edge more softly. The width is built into Sodium's terrain shader, so Done
reloads the resources once when it changed. On Minecraft 1.21.1 (one fog for everything) a width can
only make the fog start earlier, never later, so water and lava fog stay as they are. Mod Menu is
optional.

## Supported versions

One jar per Minecraft version; each works with these Sodium versions (pre-releases in between included):

| Minecraft | Sodium |
|---|---|
| 26.3 | 0.9.2 – 0.9.3 |
| 26.2 | 0.9.0 – 0.9.2 |
| 26.1.2 | 0.8.12 – 0.9.2 |
| 1.21.11 | 0.8.12 – 0.8.15-beta.1 |
| 1.21 – 1.21.1 | 0.8.12 – 0.8.13 |

The mod hooks into Sodium's terrain renderer and shader, which change between Sodium versions. Each jar
lists the exact Sodium versions it was checked against (`sky-fade.versions.properties`); with any other
Sodium version the mod stays off and writes one line to the log.

On Minecraft 1.21.1 the game has a single fog (no separate render distance fog), so there the fog
keeps its strength and its colour becomes the sky behind it.

## Credit

The idea and the approach come from the "fog override" option of
[Chunks Fade In](https://github.com/kerudion/chunksfadein) by kerudion (GPLv3). Chunks Fade In swaps
the fog colour for the sky from the start of the render distance fog; with a strong fog colour from
another mod that shows as a line where the swap starts. Sky Fade keeps the normal fog and fades from
it into the sky, and does only that.

## Building

26.x: `./gradlew build -Pmc=26.3` (or `26.2`, `26.1.2`; JDK 25), jar in `build/libs`.
1.21.x: `./gradlew -p mc1.21.11 build` (or `mc1.21.1`; JDK 21), jar in `mc1.21.11/build/libs`.

## License

GPL-3.0-only, see [LICENSE](LICENSE).
