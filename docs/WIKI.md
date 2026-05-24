# Wiki

## What This Mod Does

Structure Spread Tweaks changes how far apart structures generate by editing Minecraft structure set data before it is decoded.

## Quick Start

1. Install the mod with Fabric Loader.
2. Install Cloth Config and Mod Menu.
3. Open **Mods > Structure Spread Tweaks > Config**.
4. Set **Global spread factor**.
5. Create a new world or load new terrain.

## Recommended Values

- `1.5`: slightly less structure spam
- `2.0`: noticeably rarer structures
- `4.0`: wide exploration-focused worlds
- `0.0`: disable affected random-spread structures

## Troubleshooting

### The config changed but old chunks look the same

Existing chunks are already generated. Test in new chunks or a new world.

### The config screen is missing

Install Mod Menu and Cloth Config.

### A structure still generates too often

Add a custom override for the structure set or structure id.

### Strongholds or ring-based structures did not change

Concentric-ring placement is skipped intentionally.
