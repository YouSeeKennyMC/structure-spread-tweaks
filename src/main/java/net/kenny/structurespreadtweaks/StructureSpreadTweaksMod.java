package net.kenny.structurespreadtweaks;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructureSpreadTweaksMod implements ModInitializer, PreLaunchEntrypoint {
    public static final String MOD_ID = "structure_spread_tweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("structure_spread_tweaks.json5");

    private static volatile Config cachedConfig;

    public static Path configPath() {
        return CONFIG_PATH;
    }

    public static Config currentConfig() {
        return getConfig();
    }

    public static Config defaultConfig() {
        return Config.defaults();
    }

    public static Config reloadConfig() {
        Config config = loadConfigFromDisk();
        cachedConfig = config;
        return config;
    }

    public static void saveConfig(Config config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, configText(config), StandardCharsets.UTF_8);
            cachedConfig = config;
            LOGGER.info("Saved config. New values apply to the next world load or datapack reload.");
        } catch (Exception exception) {
            throw new RuntimeException("Failed to save Structure Spread Tweaks config file: " + CONFIG_PATH, exception);
        }
    }

    @Override
    public void onPreLaunch() {
        Config.ensureExists();
    }

    @Override
    public void onInitialize() {
        Config config = getConfig();
        if (!config.enabled()) {
            LOGGER.info("Structure Spread Tweaks is disabled.");
            return;
        }

        LOGGER.info(
                "Loaded. global={}, factor={}, vanilla={}, modded={}, customOverrides={}",
                config.useGlobalSpreadFactor(),
                config.globalSpreadFactor(),
                config.affectVanillaStructures(),
                config.affectModdedStructures(),
                config.useCustomOverrides()
        );
    }

    public static void patchStructureSetJson(ResourceKey<?> elementKey, JsonElement json) {
        Config config = getConfig();
        if (!config.enabled() || !isStructureSetKey(elementKey) || !json.isJsonObject()) {
            return;
        }

        JsonObject root = json.getAsJsonObject();
        JsonObject placement = root.getAsJsonObject("placement");
        if (placement == null) {
            return;
        }

        JsonElement type = placement.get("type");
        if (type != null && "minecraft:concentric_rings".equals(type.getAsString())) {
            return;
        }

        if (!config.affects(elementKey)) {
            return;
        }

        double factor = getSpreadFactor(config, elementKey, root);
        if (factor < 0.0D) {
            LOGGER.warn("Ignoring negative spread factor {} for {}", factor, elementKey.identifier());
            return;
        }

        if (factor == 0.0D) {
            placement.addProperty("frequency", 0.0D);
            return;
        }

        patchRandomSpreadPlacement(elementKey, placement, factor, config.changeStructureSalt());
    }

    private static Config getConfig() {
        Config config = cachedConfig;
        if (config != null) {
            return config;
        }

        synchronized (StructureSpreadTweaksMod.class) {
            config = cachedConfig;
            if (config == null) {
                config = loadConfigFromDisk();
                cachedConfig = config;
            }
        }

        return config;
    }

    private static Config loadConfigFromDisk() {
        return Config.load();
    }

    private static boolean isStructureSetKey(ResourceKey<?> elementKey) {
        return "worldgen/structure_set".equals(elementKey.registryKey().identifier().getPath());
    }

    private static double getSpreadFactor(Config config, ResourceKey<?> elementKey, JsonObject structureSetJson) {
        String structureSetId = elementKey.identifier().toString();
        if (config.useCustomOverrides()) {
            Double factor = config.customOverrides().get(structureSetId);
            if (factor != null) {
                return factor;
            }
        }

        if (config.useCustomOverrides()) {
            JsonElement structuresElement = structureSetJson.get("structures");
            if (structuresElement != null && structuresElement.isJsonArray()) {
                JsonArray structures = structuresElement.getAsJsonArray();
                for (JsonElement element : structures) {
                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonElement structure = element.getAsJsonObject().get("structure");
                    if (structure == null) {
                        continue;
                    }

                    Double factor = config.customOverrides().get(structure.getAsString());
                    if (factor != null) {
                        return factor;
                    }
                }
            }
        }

        return config.useGlobalSpreadFactor() ? config.globalSpreadFactor() : 1.0D;
    }

    private static void patchRandomSpreadPlacement(ResourceKey<?> elementKey, JsonObject placement, double factor, boolean idBasedSalt) {
        if (placement.has("spacing")) {
            int spacing = scaledPlacementValue(placement.get("spacing").getAsDouble(), factor);
            placement.addProperty("spacing", spacing);
        }

        if (placement.has("separation")) {
            int separation = scaledPlacementValue(placement.get("separation").getAsDouble(), factor);
            int spacing = placement.has("spacing") ? placement.get("spacing").getAsInt() : Math.max(1, separation + 1);
            if (separation >= spacing) {
                separation = Math.max(0, spacing - 1);
            }
            placement.addProperty("separation", separation);
        }

        if (idBasedSalt) {
            placement.addProperty("salt", elementKey.identifier().toString().hashCode() & 0x7fffffff);
        }
    }

    private static int scaledPlacementValue(double value, double factor) {
        double scaled = value * factor;
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) scaled);
    }

    public record Config(
            boolean enabled,
            boolean useGlobalSpreadFactor,
            double globalSpreadFactor,
            boolean affectVanillaStructures,
            boolean affectModdedStructures,
            boolean changeStructureSalt,
            boolean useCustomOverrides,
            LinkedHashMap<String, Double> customOverrides
    ) {
        public Config {
            customOverrides = customOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(customOverrides);
        }

        private boolean affects(ResourceKey<?> elementKey) {
            boolean vanilla = "minecraft".equals(elementKey.identifier().getNamespace());
            return vanilla ? affectVanillaStructures : affectModdedStructures;
        }

        private static Config defaults() {
            return new Config(
                    true,
                    true,
                    4.0D,
                    true,
                    true,
                    true,
                    false,
                    new LinkedHashMap<>()
            );
        }

        private static void ensureExists() {
            if (Files.isRegularFile(CONFIG_PATH)) {
                return;
            }

            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, defaultConfigText(), StandardCharsets.UTF_8);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to create Structure Spread Tweaks config file.", exception);
            }
        }

        private static Config load() {
            ensureExists();

            try {
                String contents = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(stripJsonComments(contents)).getAsJsonObject();

                Config defaults = defaults();
                boolean enabled = getBoolean(root, "enabled", defaults.enabled());
                boolean useGlobalSpreadFactor = getBoolean(root, "useGlobalSpreadFactor", defaults.useGlobalSpreadFactor());
                double globalSpreadFactor = getDouble(root, "globalSpreadFactor", defaults.globalSpreadFactor());
                boolean affectVanillaStructures = getBoolean(root, "affectVanillaStructures", defaults.affectVanillaStructures());
                boolean affectModdedStructures = getBoolean(root, "affectModdedStructures", defaults.affectModdedStructures());
                boolean changeStructureSalt = getBoolean(root, "changeStructureSalt", getBoolean(root, "idBasedSalt", defaults.changeStructureSalt()));
                boolean useCustomOverrides = getBoolean(root, "useCustomOverrides", defaults.useCustomOverrides());

                LinkedHashMap<String, Double> customOverrides = new LinkedHashMap<>();
                JsonElement overridesElement = root.get("customOverrides");
                if (overridesElement == null) {
                    overridesElement = root.get("targets");
                }
                if (overridesElement != null && overridesElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : overridesElement.getAsJsonObject().entrySet()) {
                        customOverrides.put(entry.getKey(), entry.getValue().getAsDouble());
                    }
                }

                return new Config(
                        enabled,
                        useGlobalSpreadFactor,
                        globalSpreadFactor,
                        affectVanillaStructures,
                        affectModdedStructures,
                        changeStructureSalt,
                        useCustomOverrides,
                        customOverrides
                );
            } catch (Exception exception) {
                throw new RuntimeException("Structure Spread Tweaks config file is malformed: " + CONFIG_PATH, exception);
            }
        }

        private static boolean getBoolean(JsonObject root, String key, boolean defaultValue) {
            JsonElement element = root.get(key);
            return element == null ? defaultValue : element.getAsBoolean();
        }

        private static double getDouble(JsonObject root, String key, double defaultValue) {
            JsonElement element = root.get(key);
            return element == null ? defaultValue : element.getAsDouble();
        }
    }

    private static String stripJsonComments(String input) {
        StringBuilder output = new StringBuilder(input.length());
        boolean inString = false;
        boolean escaping = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            char next = i + 1 < input.length() ? input.charAt(i + 1) : '\0';

            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                    output.append(current);
                }
                continue;
            }

            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                output.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                output.append(current);
            } else if (current == '/' && next == '/') {
                lineComment = true;
                i++;
            } else if (current == '/' && next == '*') {
                blockComment = true;
                i++;
            } else {
                output.append(current);
            }
        }

        return output.toString();
    }

    private static String defaultConfigText() {
        return configText(Config.defaults());
    }

    private static String configText(Config config) {
        StringBuilder builder = new StringBuilder();
        builder.append("// Structure Spread Tweaks\n");
        builder.append("// Manual file edits need a world reload or game restart.\n");
        builder.append("// In-game Mod Menu changes apply to the next world load or datapack reload.\n");
        builder.append("// A factor of 1.0 keeps normal spacing. Higher values make structures rarer.\n");
        builder.append("{\n");
        appendBooleanProperty(builder, "enabled", config.enabled(), true);
        appendBooleanProperty(builder, "useGlobalSpreadFactor", config.useGlobalSpreadFactor(), true);
        appendDoubleProperty(builder, "globalSpreadFactor", config.globalSpreadFactor(), true);
        appendBooleanProperty(builder, "affectVanillaStructures", config.affectVanillaStructures(), true);
        appendBooleanProperty(builder, "affectModdedStructures", config.affectModdedStructures(), true);
        appendBooleanProperty(builder, "changeStructureSalt", config.changeStructureSalt(), true);
        appendBooleanProperty(builder, "useCustomOverrides", config.useCustomOverrides(), true);
        builder.append("  \"customOverrides\": {");
        if (!config.customOverrides().isEmpty()) {
            builder.append("\n");
            int index = 0;
            for (Map.Entry<String, Double> entry : config.customOverrides().entrySet()) {
                builder.append("    \"").append(escapeJson(entry.getKey())).append("\": ").append(entry.getValue());
                if (++index < config.customOverrides().size()) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append("  }\n");
        } else {
            builder.append("}\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static void appendBooleanProperty(StringBuilder builder, String key, boolean value, boolean comma) {
        builder.append("  \"").append(key).append("\": ").append(value);
        if (comma) {
            builder.append(",");
        }
        builder.append("\n");
    }

    private static void appendDoubleProperty(StringBuilder builder, String key, double value, boolean comma) {
        builder.append("  \"").append(key).append("\": ").append(value);
        if (comma) {
            builder.append(",");
        }
        builder.append("\n");
    }

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
