# HenClient Reborn

A client-side Fabric utility mod for **Minecraft 1.21.11** with a red-and-black GUI, a clean HUD, and exploration / combat helpers.

| | |
|---|---|
| Minecraft | 1.21.11 (Mojang mappings, Loom `fabric-loom-remap`) |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.141.6+1.21.11 |
| Java | 21 |
| Build | Gradle 9.5.1 via the included wrapper |

## Modules

**HUD & Info:** Coordinates, FPS Counter, Health HUD, Sprint Status, Armor Status (low-durability warning), Direction, Biome.

**Exploration:** Storage Finder, Spawner Finder, Sus Chunk Finder (heuristic), Storage ESP, Freecam.

**Combat:** Auto Clicker, Aim Assist.

## Using it

- **Right Shift** opens the GUI (change it with the *Menu key* button in the title bar).
- Left-click a module to toggle it, right-click to only select it. The settings panel on the right shows its options, a keybind button (click it, then press a key; Backspace/Delete clears) and notes.
- Settings, keybinds and enabled states are saved to `config/henclient-reborn.json`.

## Notes and limitations

- **Combat utilities:** many servers prohibit auto-clicking and aim assistance, and server-side anticheat can detect or limit them. Use them only where they are allowed.
- **Sus Chunk Finder** is a heuristic: it scores chunks by counting chests, spawners, obsidian, cobblestone, mossy cobblestone, torches and rails below a chosen Y level. A high score is a hint, not proof.
- **Finders** only search chunks the client has already loaded and never load new ones. Scanning is interval-based and capped.
- **Storage ESP** draws screen-space boxes (projected from the camera and FOV setting) instead of using a custom world render pipeline, so they show through walls and follow Freecam. They ignore temporary FOV effects such as sprinting.
- **Freecam** moves a hidden camera entity while your body stands still. Clicking while in Freecam still acts from the camera's view, so avoid interacting. Disabling it, leaving the world or disconnecting restores the camera, input and your original view direction.
- No mixins are used.

## Build with GitHub Actions (no local Gradle needed)

1. Push this folder to your GitHub repository's `main` branch.
2. Open the repository on GitHub, click the **Actions** tab, and select **Build HenClient Reborn**. (You can also click **Run workflow** to start it manually.)
3. Open the finished run, scroll to **Artifacts** at the bottom of the summary page and download **HenClient-Reborn** (a zip containing the mod JAR).
4. Unzip it and put `henclient-reborn-1.0.0.jar` in your `.minecraft/mods` folder together with Fabric API.

### Pushing the code

```bash
git init
git add .
git commit -m "HenClient Reborn"
git branch -M main
git remote add origin https://github.com/<your-user>/<your-repo>.git
git push -u origin main --force
```

(`--force` replaces the old broken code in the repository. Drop it if you want to keep the history.)

## Local build (optional)

```bash
./gradlew build
```

The mod JAR is written to `build/libs/`.
