# PathHint Clone (Fabric 1.21.5)

A from-scratch Fabric client mod inspired by PathHint: A* pathfinding to
coordinates/waypoints, a glowing through-walls path line, break/place
block hints, death markers, breadcrumb trail, and a cave-escape button.
Press **H** in-game to open the menu.

> **Disclaimer & Credits**  
> This is an unofficial, community-maintained build/clone inspired by **PathHint**.
> * **Original Concept:** Inspired by **PathHint**.
> * **Licensing Note:** All code in this repository has been updated and recompiled to support modern Fabric API standards on Minecraft 1.21.5. This project is provided completely free of charge.
> * **Notice to Original Author:** If you are the original creator and have any concerns regarding this repository, please open an issue or contact me.

## Requirements

- **JDK 21** (Minecraft 1.21.5 requires it) — [Adoptium builds here](https://adoptium.net/temurin/releases/?version=21)
- Internet access (Gradle will download Minecraft 1.21.5, Yarn mappings,
  Fabric Loader, and Fabric API automatically on first build)

## Build

```bash
cd pathhint-clone

# Build using the Gradle wrapper:
./gradlew build        # macOS/Linux
gradlew.bat build       # Windows


The finished mod jar will be at: Releases or Actions Tab

```
build/libs/pathhint-clone-1.0.0.jar
```

That's the file you drop into your `.minecraft/mods` folder (alongside
**Fabric API 0.128.1+1.21.5** and **Fabric Loader 0.16.10+**, both
required).

## Opening the project in an IDE (recommended)

IntelliJ IDEA (Community is fine) has the smoothest Fabric workflow:

1. `File → Open` and select the `pathhint-clone` folder (the one with `build.gradle`).
2. Let Gradle sync — this downloads everything and generates `gradlew` for you.
3. Run the `runClient` Gradle task (or the auto-generated "Minecraft Client" run
   configuration) to test the mod in a real dev client before packaging it.
4. Run the `build` Gradle task to produce the jar.

## Project layout

```
src/main/java/com/pathhintclone/
  PathHintClient.java      - entrypoint: keybind, tick loop, death detection
  PathHintState.java       - shared in-memory state (path, breadcrumbs, target)
  PathfindingEngine.java   - A* over the block grid (walk/jump/fall/swim/climb,
                              break & place hints when a route needs them)
  Waypoint.java            - simple saved-location record
  WaypointManager.java     - JSON persistence in config/pathhintclone/
  render/PathRenderer.java - glowing path line, hint outlines, breadcrumbs, beacon
  render/HudOverlay.java   - distance/height readout
  gui/PathHintScreen.java  - the H-key menu
```

## Notes / things worth tuning

- **Pathfinding range** is capped at 200 blocks and 15,000 explored nodes
  (`PathfindingEngine.MAX_RANGE` / `MAX_NODES`) to keep it fast — raise those
  if you want longer-range routes at the cost of a slower search.
- **Recalculation** happens once a second while a path is active
  (`PathHintClient.RECALC_INTERVAL`). Lower it for snappier updates to a
  moving target, at a small perf cost.
- Minecraft's rendering internals (`RenderLayer`/pipeline setup) change
  fairly often between versions — `PathRenderer` is the piece most likely
  to need a small tweak if you port this to a different Minecraft version.
- The waypoint list in the menu doesn't scroll yet — past ~8 waypoints it
  will just stop drawing new rows. Worth adding a `ScrollableWidget` if you
  save a lot of them.
- The GUI here is intentionally plain (no textures/icons) so it depends on
  nothing beyond vanilla + Fabric API. Reskin it however you like.
