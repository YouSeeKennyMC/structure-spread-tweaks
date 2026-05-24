# Configuration

The config file is created at:

```text
config/structure_spread_tweaks.json5
```

You can edit it in-game through Mod Menu, or edit the file manually.

In-game saves update the mod's live config cache immediately. The new values are used the next time Minecraft loads registry data, such as when opening a new world or reloading datapacks.

Manual file edits need a world reload, datapack reload, or game restart.

## Options

### `enabled`

Turns all changes on or off.

### `useGlobalSpreadFactor`

When enabled, the global factor is used for every affected structure set unless a custom override matches.

### `globalSpreadFactor`

Controls how far apart random-spread structures generate.

- `1.0`: normal spacing
- `2.0`: roughly twice as rare
- `4.0`: roughly four times as rare
- `0.0`: disables affected random-spread structures

### `affectVanillaStructures`

Controls whether Minecraft's built-in structure sets are changed.

### `affectModdedStructures`

Controls whether structure sets from mods and datapacks are changed.

### `changeStructureSalt`

Replaces the placement salt with a value based on the structure set id. This can reduce overlap when mods reuse salts.

### `useCustomOverrides`

Enables the `customOverrides` map.

### `customOverrides`

Maps a structure set id or structure id to a spread factor.

```json5
"customOverrides": {
  "minecraft:villages": 3.0,
  "minecraft:mansion": 6.0,
  "terralith:ancient_city": 2.5
}
```

If the key matches a structure set id, that set uses the factor.

If the key matches a structure id inside a structure set, the whole containing structure set uses the factor.

## Notes

This mod patches random-spread placement data. It skips concentric-ring structure placement, such as stronghold-style placement, because that uses a different placement model.
