# Structure Spread Tweaks

![Structure Spread Tweaks logo](docs/images/logo.png)

Structure Spread Tweaks is a small Fabric mod for Minecraft `26.1.x` that lets modpack authors make structures generate farther apart, closer together, or disable selected random-spread structures entirely.

It adjusts structure set placement data while Minecraft prepares world generation.

## Features

- Global spread factor for vanilla and modded structures
- Separate toggles for vanilla and modded structure sets
- Optional per-structure or per-structure-set overrides
- Optional id-based salt replacement to reduce structure overlap
- Mod Menu + Cloth Config screen for in-game editing
- In-game saves apply to the next world load or datapack reload without restarting Minecraft

## Requirements

- Minecraft `26.1.x`
- Fabric Loader `0.19.2` or newer
- Java `25` or newer
- [Cloth Config API](https://modrinth.com/mod/cloth-config) for the in-game config screen
- [Mod Menu](https://modrinth.com/mod/modmenu) to open the in-game config screen

The core structure patching runs without Fabric API events, but [Fabric API](https://modrinth.com/mod/fabric-api) is recommended in normal modpack environments.

## Usage

Open **Mods > Structure Spread Tweaks > Config** with Mod Menu.

The most common setting is:

```json5
"globalSpreadFactor": 4.0
```

A factor of `1.0` keeps normal spacing. Higher values make structures rarer. `0.0` disables random-spread structures affected by the setting.

Existing chunks are not regenerated. Test changes in new terrain, a new world, or after deleting generated chunks.

## Custom Overrides

Overrides use structure set ids or structure ids:

```json5
"customOverrides": {
  "minecraft:villages": 3.0,
  "minecraft:mansion": 6.0
}
```

See [Configuration](docs/CONFIGURATION.md) for details.

## Building

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew build
```

The mod jar is written to:

```text
build/libs/
```

## Documentation

- [Configuration](docs/CONFIGURATION.md)
- [Technical notes](docs/TECHNICAL_NOTES.md)
- [Wiki home](docs/WIKI.md)
- [Publishing](docs/PUBLISHING.md)
- [Changelog](CHANGELOG.md)

## License

MIT. See [LICENSE](LICENSE).
