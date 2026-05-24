# Technical Notes

Minecraft loads `worldgen/structure_set` data from JSON and decodes it into runtime `StructureSet` objects.

Structure Spread Tweaks injects into:

```text
net.minecraft.resources.RegistryLoadTask$PendingRegistration#loadFromResource
```

The patch runs immediately before:

```java
Decoder#parse(...)
```

That timing matters. The raw `JsonElement` must be changed before Minecraft decodes it. If the JSON is changed after `Decoder#parse(...)`, the config can appear to load correctly while world generation still uses the original spacing.

## Minecraft 26.1.2 Local Capture

In Minecraft `26.1.2`, the reader local captured by the mixin is `java.io.Reader`.

Using `java.io.BufferedReader` in the callback signature causes an LVT mismatch:

```text
Expected: BufferedReader, JsonElement
Found:    Reader, JsonElement
```

The compatible callback captures:

```java
Reader reader,
JsonElement json
```

## Runtime Config

The config is cached for registry loading performance. In-game saves replace the cached config immediately, so the next world load or datapack reload uses the new values without restarting Minecraft.
