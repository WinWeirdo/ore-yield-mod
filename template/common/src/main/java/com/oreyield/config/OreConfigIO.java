package com.oreyield.config;

import com.oreyield.compat.ModCompat2Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hand-rolled TOML reader/writer for the ore_yield.toml config file. No platform config API involved. */
public final class OreConfigIO {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/Config");
    private static final List<String> ORE_SECTION_KEYS = List.of(
            "enabled", "host_blocks", "result_item", "min_count", "max_count", "chance",
            "min_y", "max_y", "peak_y", "fortune_type", "xp_min", "xp_max", "dimension",
            "min_pickaxe_level");
    private static final List<String> LEGACY_TOP_LEVEL_KEYS = List.of(
            "stone_generator_cooldown", "stone_generator_interval_ms");

    // Retained when an old config is migrated or later saved from the in-game screen.
    // This prevents a new Ore Yield version from deleting server-owner extensions.
    private static Map<String, String> preservedTopLevel = Map.of();
    private static Map<String, Map<String, String>> preservedOreFields = Map.of();
    private static Map<String, Map<String, String>> preservedSections = Map.of();

    private OreConfigIO() {}

    public static void load(Path configFile) {
        OreConfig.setConfigPath(configFile);
        OreConfig.clearOverrides();
        preservedTopLevel = Map.of();
        preservedOreFields = Map.of();
        preservedSections = Map.of();
        if (!Files.exists(configFile)) {
            save(configFile);
            OreConfig.rebuild();
            return;
        }
        List<String> missingSettings = List.of();
        List<String> missingOreSections = List.of();
        List<String> missingOreFields = List.of();
        boolean loaded = false;
        try {
            String section = "";
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            Map<String, String> sectionValues = null;
            Map<String, String> top = new LinkedHashMap<>();
            for (String raw : Files.readAllLines(configFile)) {
                String line = stripInlineComment(raw).strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (sectionValues != null && !section.isEmpty()) {
                        sections.put(section, sectionValues);
                    }
                    section = line.substring(1, line.length() - 1).strip();
                    sectionValues = new LinkedHashMap<>();
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (section.isEmpty()) {
                    top.put(key, value);
                } else if (sectionValues != null) {
                    sectionValues.put(key, value);
                }
            }
            if (sectionValues != null && !section.isEmpty()) {
                sections.put(section, sectionValues);
            }

            OreConfig.setValue("remove_vanilla_ore_generation", bool(top, "remove_vanilla_ore_generation", false));
            OreConfig.setValue("remove_compatible_ore_generation", bool(top, "remove_compatible_ore_generation", true));
            OreConfig.setValue("enable_mod_compat", bool(top, "enable_mod_compat", false));
            OreConfig.setValue("enable_mod_compat_2", bool(top, "enable_mod_compat_2", true));
            OreConfig.setValue("mod_compat_2_ores_in_end", bool(top, "mod_compat_2_ores_in_end", true));
            OreConfig.setValue("enable_vanilla_end_ores", bool(top, "enable_vanilla_end_ores", true));
            OreConfig.setValue("auto_detect_dimensions", bool(top, "auto_detect_dimensions", true));
            OreConfig.setValue("bad_luck_eliminator", bool(top, "bad_luck_eliminator", true));
            OreConfig.setValue("bad_luck_multiplier", decimal(top, "bad_luck_multiplier", 2.0D));
            OreConfig.setValue("enable_mineral_pockets", bool(top, "enable_mineral_pockets", false));
            OreConfig.setValue("mineral_pockets_end_enabled", bool(top, "mineral_pockets_end_enabled", true));
            OreConfig.setValue("mineral_pockets_allow_generator", bool(top, "mineral_pockets_allow_generator", false));
            OreConfig.setValue("mineral_pockets_allow_automated_harvesting", bool(top, "mineral_pockets_allow_automated_harvesting", false));
            OreConfig.setValue("mineral_pocket_chance", decimal(top, "mineral_pocket_chance", 0.00024112121212121212D));
            OreConfig.setMineralPocketResourceTypeRange(
                    integer(top, "mineral_pocket_min_resource_types", 2),
                    integer(top, "mineral_pocket_max_resource_types", 3));
            for (MineralPocketType type : MineralPocketType.values()) {
                MineralPocketSettings defaults = OreConfig.defaultMineralPocketSettings(type);
                String prefix = "mineral_pocket_" + type.configKey() + "_";
                OreConfig.setMineralPocketSettings(type,
                        bool(top, prefix + "enabled", defaults.enabled()),
                        integer(top, prefix + "weight", defaults.weight()),
                        integer(top, prefix + "min_count", defaults.minCount()),
                        integer(top, prefix + "max_count", defaults.maxCount()));
            }
            boolean antiCheeseEnabled = bool(top, "enable_anti_cheese_mechanics", false);
            OreConfig.setValue("stone_generator_enabled", bool(top, "stone_generator_enabled", antiCheeseEnabled));
            OreConfig.setValue("generator_tracking_blocks_enabled", bool(top, "generator_tracking_blocks_enabled", false));
            OreConfig.setValue("generator_ore_yield_enabled", bool(top, "generator_ore_yield_enabled", true));
            OreConfig.setValue("generator_ore_yield_chance_multiplier", decimal(top, "generator_ore_yield_chance_multiplier", 0.25D));
            OreConfig.setValue("enable_anti_cheese_mechanics", antiCheeseEnabled);
            OreConfig.setValue("allow_player_placed_eligible_blocks", bool(top, "allow_player_placed_eligible_blocks", true));
            OreConfig.setValue("allow_generator_automated_harvesting", bool(top, "allow_generator_automated_harvesting", false));
            OreConfig.setValue("allow_generator_explosion_harvesting", bool(top, "allow_generator_explosion_harvesting", false));
            loadStoneGeneratorCooldown(top);
            OreConfig.setStoneGeneratorRecipeItems(string(top, "stone_generator_surrounding_item", "minecraft:diamond"),
                    string(top, "stone_generator_center_item", "minecraft:end_stone"));
            OreConfig.setEnabledDimensions(strList(top, "enabled_dimensions", List.of()));
            OreConfig.setAutoDetectedDimensions(strList(top, "auto_detected_dimensions", List.of()));
            OreConfig.setAdditionalOres(strList(top, "additional_ores", List.of()));
            for (Map.Entry<String, Map<String, String>> entry : sections.entrySet()) {
                if (entry.getKey().startsWith("ore.")) {
                    OreConfig.applyOverrides(entry.getKey().substring(4), entry.getValue());
                }
            }
            capturePreservedValues(top, sections);
            missingSettings = missingTopLevelSettings(top);
            missingOreSections = missingOreSections(sections);
            missingOreFields = missingOreFields(sections);
            loaded = true;
        } catch (IOException e) {
            LOGGER.warn("[Ore Yield] Failed to read config file {}: {}", configFile, e.getMessage());
        }
        OreConfig.rebuild();
        if (loaded && (!missingSettings.isEmpty() || !missingOreSections.isEmpty() || !missingOreFields.isEmpty())) {
            LOGGER.info("[Ore Yield] Updating {} with {} missing setting(s), {} missing ore section(s), and {} missing ore field(s); existing values are preserved.",
                    configFile, missingSettings.size(), missingOreSections.size(), missingOreFields.size());
            save(configFile);
        }
    }

    public static void save(Path configFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Ore Yield configuration\n");
        sb.append("# Set by the in-game config screen or edit manually, then use \"Reload from File\".\n\n");
        line(sb, "remove_vanilla_ore_generation", OreConfig.shouldRemoveVanillaOreGeneration());
        sb.append("# With vanilla worldgen removal enabled, applies only to exact Create/Mekanism 1.20.1 ore features.\n");
        line(sb, "remove_compatible_ore_generation", OreConfig.shouldRemoveCompatibleOreGeneration());
        line(sb, "enable_mod_compat", OreConfig.isModCompatEnabled());
        line(sb, "enable_mod_compat_2", OreConfig.isModCompat2Enabled());
        line(sb, "mod_compat_2_ores_in_end", OreConfig.isModCompat2OresInEnd());
        line(sb, "enable_vanilla_end_ores", OreConfig.isVanillaEndOresEnabled());
        line(sb, "auto_detect_dimensions", OreConfig.isAutoDetectDimensionsEnabled());
        line(sb, "bad_luck_eliminator", OreConfig.isBadLuckEliminatorEnabled());
        line(sb, "bad_luck_multiplier", OreConfig.badLuckMultiplier());
        sb.append("# Rare mineral pockets are separate from normal ore rolls and never use Bad Luck Eliminator or Fortune.\n");
        sb.append("# They are disabled by default. The Nether is intentionally excluded.\n");
        line(sb, "enable_mineral_pockets", OreConfig.isMineralPocketsEnabled());
        line(sb, "mineral_pockets_end_enabled", OreConfig.isMineralPocketsInEndEnabled());
        sb.append("# Marked Stone Generator output and non-player harvesting remain disabled by default. Explosions never roll pockets.\n");
        line(sb, "mineral_pockets_allow_generator", OreConfig.allowsMineralPocketsOnGenerator());
        line(sb, "mineral_pockets_allow_automated_harvesting", OreConfig.allowsMineralPocketAutomatedHarvesting());
        sb.append("# Master chance is approximately one pocket per 4,147 eligible breaks when all category weights are enabled.\n");
        line(sb, "mineral_pocket_chance", OreConfig.mineralPocketChance());
        sb.append("# Metal and gem pockets choose this many distinct installed resources. Counts below apply to each selected resource.\n");
        line(sb, "mineral_pocket_min_resource_types", OreConfig.mineralPocketMinResourceTypes());
        line(sb, "mineral_pocket_max_resource_types", OreConfig.mineralPocketMaxResourceTypes());
        for (MineralPocketType type : MineralPocketType.values()) appendMineralPocket(sb, type);
        sb.append("\n");
        sb.append("# Stone Generator: cooldown in server ticks (20 ticks = one second).\n");
        line(sb, "stone_generator_cooldown_ticks", OreConfig.stoneGeneratorCooldownTicks());
        sb.append("# Disabled by default so Ore Yield remains server-side: clients do not need this mod.\n");
        sb.append("# Enables Stone Generator content; every client must install Ore Yield and restart.\n");
        line(sb, "stone_generator_enabled", OreConfig.isStoneGeneratorEnabled());
        sb.append("# Disabled by default. When off, the Generator creates normal vanilla host blocks.\n");
        sb.append("# Enable only for Generator-specific yield, automation, explosion, and pocket controls.\n");
        line(sb, "generator_tracking_blocks_enabled", OreConfig.areGeneratorTrackingBlocksEnabled());
        sb.append("# Tracks player-placed eligible blocks separately; every client must install Ore Yield and restart.\n");
        line(sb, "enable_anti_cheese_mechanics", OreConfig.isAntiCheeseMechanicsEnabled());
        sb.append("# Generator output is tracked separately from world blocks and rolls Ore Yield rewards by default.\n");
        line(sb, "generator_ore_yield_enabled", OreConfig.isGeneratorOreYieldEnabled());
        sb.append("# Applied only to marked generator output when generator ore yield is enabled.\n");
        line(sb, "generator_ore_yield_chance_multiplier", OreConfig.generatorOreYieldChanceMultiplier());
        sb.append("# Player-placed eligible host blocks keep normal Ore Yield behavior by default.\n");
        line(sb, "allow_player_placed_eligible_blocks", OreConfig.allowsPlayerPlacedEligibleBlocks());
        sb.append("# Protect marked generator output from automation and explosions by default.\n");
        line(sb, "allow_generator_automated_harvesting", OreConfig.allowsGeneratorAutomatedHarvesting());
        line(sb, "allow_generator_explosion_harvesting", OreConfig.allowsGeneratorExplosionHarvesting());
        sb.append("# Recipe-only settings; intentionally not shown in the in-game config screen.\n");
        sb.append("stone_generator_surrounding_item = \"").append(OreConfig.stoneGeneratorSurroundingItem()).append("\"\n");
        sb.append("stone_generator_center_item = \"").append(OreConfig.stoneGeneratorCenterItem()).append("\"\n");
        sb.append("\n");
        sb.append("# Dimensions where the overworld ore set also drops (only active while auto detection is on).\n");
        sb.append("# Edit this list to pin a dimension or prune an auto-detected one.\n");
        sb.append("enabled_dimensions = ").append(toTomlList(OreConfig.getEnabledDimensions())).append("\n");
        sb.append("# Internal auto-detection history; keep this list so removed dimensions stay pruned.\n");
        sb.append("auto_detected_dimensions = ").append(toTomlList(OreConfig.getAutoDetectedDimensions())).append("\n");
        sb.append("\n");
        sb.append("additional_ores = ").append(toTomlList(OreConfig.getAdditionalOres())).append("\n");
        appendPreservedTopLevel(sb);
        sb.append("# One entry per modded ore: id|enabled|result_item|min_count|max_count|chance|min_y|max_y|peak_y|fortune_type|xp_min|xp_max|dimension|host1,host2|min_pickaxe_level\n\n");

        for (OreEntry entry : OreConfig.defaultEntries().values()) appendOre(sb, OreConfig.effectiveEntry(entry.id()));
        sb.append("# Optional Create/Mekanism 1.20.1 entries. They activate only when the relevant mod is installed.\n\n");
        for (OreEntry entry : ModCompat2Manager.phaseTwoDefaults()) appendOre(sb, OreConfig.effectiveEntry(entry.id()));
        appendPreservedSections(sb);

        try {
            Path parent = configFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(configFile, sb.toString());
        } catch (IOException e) {
            LOGGER.warn("[Ore Yield] Failed to write config file {}: {}", configFile, e.getMessage());
        }
    }

    private static void capturePreservedValues(Map<String, String> top, Map<String, Map<String, String>> sections) {
        List<String> currentTopLevelKeys = currentTopLevelKeys();
        List<String> currentOreSectionIds = currentOreSectionIds();
        Map<String, String> extraTopLevel = new LinkedHashMap<>();
        Map<String, Map<String, String>> extraOreFields = new LinkedHashMap<>();
        Map<String, Map<String, String>> extraSections = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : top.entrySet()) {
            if (!currentTopLevelKeys.contains(entry.getKey()) && !LEGACY_TOP_LEVEL_KEYS.contains(entry.getKey())) {
                extraTopLevel.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Map<String, String>> entry : sections.entrySet()) {
            String section = entry.getKey();
            Map<String, String> values = entry.getValue();
            if (section.startsWith("ore.") && currentOreSectionIds.contains(section.substring(4))) {
                Map<String, String> extraFields = new LinkedHashMap<>();
                for (Map.Entry<String, String> field : values.entrySet()) {
                    if (!ORE_SECTION_KEYS.contains(field.getKey())) {
                        extraFields.put(field.getKey(), field.getValue());
                    }
                }
                if (!extraFields.isEmpty()) {
                    extraOreFields.put(section, extraFields);
                }
            } else {
                extraSections.put(section, new LinkedHashMap<>(values));
            }
        }
        preservedTopLevel = extraTopLevel;
        preservedOreFields = extraOreFields;
        preservedSections = extraSections;
    }

    private static List<String> missingTopLevelSettings(Map<String, String> top) {
        List<String> missing = new ArrayList<>();
        for (String key : currentTopLevelKeys()) {
            if (!top.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    private static List<String> missingOreSections(Map<String, Map<String, String>> sections) {
        List<String> missing = new ArrayList<>();
        for (String id : currentOreSectionIds()) {
            if (!sections.containsKey("ore." + id)) {
                missing.add(id);
            }
        }
        return missing;
    }

    private static List<String> missingOreFields(Map<String, Map<String, String>> sections) {
        List<String> missing = new ArrayList<>();
        for (String id : currentOreSectionIds()) {
            Map<String, String> values = sections.get("ore." + id);
            if (values == null) continue;
            for (String key : ORE_SECTION_KEYS) {
                if (!values.containsKey(key)) {
                    missing.add(id + "." + key);
                }
            }
        }
        return missing;
    }

    private static List<String> currentTopLevelKeys() {
        List<String> keys = new ArrayList<>(List.of(
                "remove_vanilla_ore_generation", "remove_compatible_ore_generation",
                "enable_mod_compat", "enable_mod_compat_2", "mod_compat_2_ores_in_end",
                "enable_vanilla_end_ores", "auto_detect_dimensions", "bad_luck_eliminator",
                "bad_luck_multiplier", "enable_mineral_pockets", "mineral_pockets_end_enabled",
                "mineral_pockets_allow_generator", "mineral_pockets_allow_automated_harvesting",
                "mineral_pocket_chance", "mineral_pocket_min_resource_types",
                "mineral_pocket_max_resource_types", "stone_generator_cooldown_ticks",
                "stone_generator_enabled", "generator_tracking_blocks_enabled", "enable_anti_cheese_mechanics", "generator_ore_yield_enabled",
                "generator_ore_yield_chance_multiplier", "allow_player_placed_eligible_blocks",
                "allow_generator_automated_harvesting", "allow_generator_explosion_harvesting",
                "stone_generator_surrounding_item", "stone_generator_center_item",
                "enabled_dimensions", "auto_detected_dimensions", "additional_ores"));
        for (MineralPocketType type : MineralPocketType.values()) {
            String prefix = "mineral_pocket_" + type.configKey() + "_";
            keys.add(prefix + "enabled");
            keys.add(prefix + "weight");
            keys.add(prefix + "min_count");
            keys.add(prefix + "max_count");
        }
        return keys;
    }

    private static List<String> currentOreSectionIds() {
        List<String> ids = new ArrayList<>(OreConfig.defaultEntries().keySet());
        for (OreEntry entry : ModCompat2Manager.phaseTwoDefaults()) {
            if (!ids.contains(entry.id())) {
                ids.add(entry.id());
            }
        }
        return ids;
    }

    private static void appendPreservedTopLevel(StringBuilder sb) {
        if (preservedTopLevel.isEmpty()) return;
        sb.append("# Preserved unrecognized settings from an older config.\n");
        for (Map.Entry<String, String> entry : preservedTopLevel.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
    }

    private static void appendPreservedSections(StringBuilder sb) {
        if (preservedSections.isEmpty()) return;
        sb.append("# Preserved unrecognized config sections from an older config.\n\n");
        for (Map.Entry<String, Map<String, String>> entry : preservedSections.entrySet()) {
            sb.append("[").append(entry.getKey()).append("]\n");
            for (Map.Entry<String, String> value : entry.getValue().entrySet()) {
                sb.append(value.getKey()).append(" = ").append(value.getValue()).append("\n");
            }
            sb.append("\n");
        }
    }

    private static void line(StringBuilder sb, String key, boolean value) {
        sb.append(key).append(" = ").append(value).append("\n");
    }

    private static void line(StringBuilder sb, String key, double value) {
        sb.append(key).append(" = ").append(value).append("\n");
    }

    private static void line(StringBuilder sb, String key, int value) {
        sb.append(key).append(" = ").append(value).append("\n");
    }

    private static void appendMineralPocket(StringBuilder sb, MineralPocketType type) {
        MineralPocketSettings settings = OreConfig.mineralPocketSettings(type);
        String prefix = "mineral_pocket_" + type.configKey() + "_";
        line(sb, prefix + "enabled", settings.enabled());
        line(sb, prefix + "weight", settings.weight());
        line(sb, prefix + "min_count", settings.minCount());
        line(sb, prefix + "max_count", settings.maxCount());
    }

    private static void appendOre(StringBuilder sb, OreEntry entry) {
        if (entry == null) return;
        sb.append("[ore.").append(entry.id()).append("]\n");
        line(sb, "enabled", entry.enabled());
        sb.append("host_blocks = ").append(toTomlList(entry.hosts())).append("\n");
        sb.append("result_item = \"").append(entry.resultItem()).append("\"\n");
        sb.append("min_count = ").append(entry.minCount()).append("\n");
        sb.append("max_count = ").append(entry.maxCount()).append("\n");
        sb.append("chance = ").append(entry.chance()).append("\n");
        sb.append("min_y = ").append(entry.minY()).append("\n");
        sb.append("max_y = ").append(entry.maxY()).append("\n");
        sb.append("peak_y = ").append(entry.peakY()).append("\n");
        sb.append("fortune_type = \"").append(entry.fortuneType().name()).append("\"\n");
        sb.append("xp_min = ").append(entry.xpMin()).append("\n");
        sb.append("xp_max = ").append(entry.xpMax()).append("\n");
        sb.append("dimension = \"").append(entry.dimension()).append("\"\n");
        sb.append("min_pickaxe_level = ").append(entry.minPickaxeLevel()).append("\n");
        Map<String, String> extraFields = preservedOreFields.get("ore." + entry.id());
        if (extraFields != null) {
            for (Map.Entry<String, String> field : extraFields.entrySet()) {
                sb.append(field.getKey()).append(" = ").append(field.getValue()).append("\n");
            }
        }
        sb.append("\n");
    }

    private static String toTomlList(List<String> values) {
        if (values.isEmpty()) return "[]";
        List<String> quoted = values.stream().map(v -> "\"" + v + "\"").toList();
        return "[" + String.join(", ", quoted) + "]";
    }

    private static boolean bool(Map<String, String> map, String key, boolean def) {
        String v = map.get(key);
        if (v == null) return def;
        return v.equalsIgnoreCase("true");
    }

    private static double decimal(Map<String, String> map, String key, double def) {
        String v = map.get(key);
        if (v == null) return def;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long longValue(Map<String, String> map, String key, long def) {
        String value = map.get(key);
        if (value == null) return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("[Ore Yield] Invalid {}='{}'; using {}.", key, value, def);
            return def;
        }
    }

    private static int integer(Map<String, String> map, String key, int def) {
        String value = map.get(key);
        if (value == null) return def;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("[Ore Yield] Invalid {}='{}'; using {}.", key, value, def);
            return def;
        }
    }

    private static void loadStoneGeneratorCooldown(Map<String, String> top) {
        if (top.containsKey("stone_generator_cooldown_ticks")) {
            OreConfig.setStoneGeneratorCooldownTicks(longValue(top, "stone_generator_cooldown_ticks", 1L));
            return;
        }
        // Accept the natural shorter spelling too, then migrate it to the canonical key on the next save.
        if (top.containsKey("stone_generator_cooldown")) {
            OreConfig.setStoneGeneratorCooldownTicks(longValue(top, "stone_generator_cooldown", 1L));
            return;
        }
        // Compatibility with the original proof-of-concept setting.
        OreConfig.setStoneGeneratorIntervalMillis(longValue(top, "stone_generator_interval_ms", 50L));
    }

    private static String string(Map<String, String> map, String key, String def) {
        String value = map.get(key);
        if (value == null) return def;
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static List<String> strList(Map<String, String> map, String key, List<String> def) {
        String v = map.get(key);
        if (v == null) return def;
        if (!v.startsWith("[") || !v.endsWith("]")) return def;
        List<String> out = new ArrayList<>();
        for (String part : splitTomlArray(v.substring(1, v.length() - 1))) {
            if (!part.isEmpty()) out.add(part);
        }
        return out;
    }

    /** Splits the inner contents of a TOML array on commas, honoring quoted elements (which may contain commas). */
    private static List<String> splitTomlArray(String inner) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                parts.add(unquote(current.toString().strip()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(unquote(current.toString().strip()));
        return parts;
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Removes a TOML comment while preserving hash characters inside quoted values. */
    private static String stripInlineComment(String value) {
        boolean inQuotes = false;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' && !escaped) {
                inQuotes = !inQuotes;
            } else if (character == '#' && !inQuotes) {
                return value.substring(0, index).stripTrailing();
            }
            escaped = character == '\\' && !escaped;
            if (character != '\\') escaped = false;
        }
        return value;
    }
}
