package net.kenny.structurespreadtweaks.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kenny.structurespreadtweaks.StructureSpreadTweaksMod;
import net.kenny.structurespreadtweaks.StructureSpreadTweaksMod.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class StructureSpreadTweaksConfigScreen {
    private StructureSpreadTweaksConfigScreen() {
    }

    public static Screen create(Screen parent) {
        Config config = StructureSpreadTweaksMod.currentConfig();
        Config defaults = StructureSpreadTweaksMod.defaultConfig();
        ConfigState state = new ConfigState(config);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(text("Structure Spread Tweaks"))
                .setSavingRunnable(() -> StructureSpreadTweaksMod.saveConfig(state.toConfig()));
        ConfigEntryBuilder entries = builder.entryBuilder();

        var general = builder.getOrCreateCategory(text("General"));
        general.addEntry(entries.startBooleanToggle(text("Enabled"), state.enabled)
                .setDefaultValue(defaults.enabled())
                .setSaveConsumer(value -> state.enabled = value)
                .setTooltip(text("Turns all structure spacing edits on or off."))
                .build());
        general.addEntry(entries.startBooleanToggle(text("Use global spread factor"), state.useGlobalSpreadFactor)
                .setDefaultValue(defaults.useGlobalSpreadFactor())
                .setSaveConsumer(value -> state.useGlobalSpreadFactor = value)
                .setTooltip(text("Use one multiplier for every affected structure set."))
                .build());
        general.addEntry(entries.startDoubleField(text("Global spread factor"), state.globalSpreadFactor)
                .setDefaultValue(defaults.globalSpreadFactor())
                .setMin(0.0D)
                .setSaveConsumer(value -> state.globalSpreadFactor = value)
                .setErrorSupplier(value -> value < 0.0D || !Double.isFinite(value)
                        ? Optional.of(text("Use a number of 0 or higher."))
                        : Optional.empty())
                .setTooltip(
                        text("1 keeps normal spacing."),
                        text("Higher values make structures rarer."),
                        text("0 disables random-spread structures.")
                )
                .build());
        general.addEntry(entries.startBooleanToggle(text("Affect vanilla structures"), state.affectVanillaStructures)
                .setDefaultValue(defaults.affectVanillaStructures())
                .setSaveConsumer(value -> state.affectVanillaStructures = value)
                .build());
        general.addEntry(entries.startBooleanToggle(text("Affect modded structures"), state.affectModdedStructures)
                .setDefaultValue(defaults.affectModdedStructures())
                .setSaveConsumer(value -> state.affectModdedStructures = value)
                .build());
        general.addEntry(entries.startBooleanToggle(text("Change structure salt"), state.changeStructureSalt)
                .setDefaultValue(defaults.changeStructureSalt())
                .setSaveConsumer(value -> state.changeStructureSalt = value)
                .setTooltip(text("Gives each structure set a salt based on its id to reduce overlap."))
                .build());

        var custom = builder.getOrCreateCategory(text("Custom overrides"));
        custom.addEntry(entries.startBooleanToggle(text("Use custom overrides"), state.useCustomOverrides)
                .setDefaultValue(defaults.useCustomOverrides())
                .setSaveConsumer(value -> state.useCustomOverrides = value)
                .setTooltip(text("Overrides can target a structure set id or a structure id."))
                .build());
        custom.addEntry(entries.startStrList(text("Overrides"), state.customOverrideLines)
                .setDefaultValue(List.of())
                .setSaveConsumer(value -> state.customOverrideLines = new ArrayList<>(value))
                .setCellErrorSupplier(StructureSpreadTweaksConfigScreen::validateOverrideLine)
                .setErrorSupplier(StructureSpreadTweaksConfigScreen::validateOverrideLines)
                .setTooltip(
                        text("Format: namespace:id = factor"),
                        text("Example: minecraft:villages = 3.5")
                )
                .setExpanded(false)
                .build());

        return builder.build();
    }

    private static Optional<Component> validateOverrideLines(List<String> lines) {
        for (String line : lines) {
            Optional<Component> error = validateOverrideLine(line);
            if (error.isPresent()) {
                return error;
            }
        }
        return Optional.empty();
    }

    private static Optional<Component> validateOverrideLine(String line) {
        try {
            parseOverrideLine(line);
            return Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.of(text(exception.getMessage()));
        }
    }

    private static LinkedHashMap<String, Double> parseOverrideLines(List<String> lines) {
        LinkedHashMap<String, Double> overrides = new LinkedHashMap<>();
        for (String line : lines) {
            OverrideEntry entry = parseOverrideLine(line);
            if (entry != null) {
                overrides.put(entry.id(), entry.factor());
            }
        }
        return overrides;
    }

    private static OverrideEntry parseOverrideLine(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isEmpty()) {
            return null;
        }

        int delimiter = line.indexOf('=');
        if (delimiter < 0) {
            delimiter = line.lastIndexOf(' ');
        }
        if (delimiter < 0) {
            throw new IllegalArgumentException("Use namespace:id = factor.");
        }

        String id = line.substring(0, delimiter).trim();
        String factorText = line.substring(delimiter + 1).trim();
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }

        try {
            double factor = Double.parseDouble(factorText);
            if (factor < 0.0D || !Double.isFinite(factor)) {
                throw new IllegalArgumentException("Factor must be 0 or higher.");
            }
            return new OverrideEntry(id, factor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid factor: " + factorText);
        }
    }

    private static List<String> overrideLines(Map<String, Double> overrides) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Double> entry : overrides.entrySet()) {
            lines.add(entry.getKey() + " = " + entry.getValue());
        }
        return lines;
    }

    private static Component text(String value) {
        return Component.literal(value);
    }

    private record OverrideEntry(String id, double factor) {
    }

    private static final class ConfigState {
        private boolean enabled;
        private boolean useGlobalSpreadFactor;
        private double globalSpreadFactor;
        private boolean affectVanillaStructures;
        private boolean affectModdedStructures;
        private boolean changeStructureSalt;
        private boolean useCustomOverrides;
        private List<String> customOverrideLines;

        private ConfigState(Config config) {
            enabled = config.enabled();
            useGlobalSpreadFactor = config.useGlobalSpreadFactor();
            globalSpreadFactor = config.globalSpreadFactor();
            affectVanillaStructures = config.affectVanillaStructures();
            affectModdedStructures = config.affectModdedStructures();
            changeStructureSalt = config.changeStructureSalt();
            useCustomOverrides = config.useCustomOverrides();
            customOverrideLines = overrideLines(config.customOverrides());
        }

        private Config toConfig() {
            return new Config(
                    enabled,
                    useGlobalSpreadFactor,
                    globalSpreadFactor,
                    affectVanillaStructures,
                    affectModdedStructures,
                    changeStructureSalt,
                    useCustomOverrides,
                    parseOverrideLines(customOverrideLines)
            );
        }
    }
}
