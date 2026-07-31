package com.oreyield.config;

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

    private OreConfigIO() {}

    public static void load(Path configFile) {
        OreConfig.setConfigPath(configFile);
        OreConfig.clearOverrides();
        if (!Files.exists(configFile)) {
            save(configFile);
            OreConfig.rebuild();
            return;
        }
        try {
            String section = "";
            Map<String, Map<String, String>> sections = new LinkedHashMap<>();
            Map<String, String> sectionValues = null;
            Map<String, String> top = new LinkedHashMap<>();
            for (String raw : Files.readAllLines(configFile)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (sectionValues != null && section.startsWith("ore.")) {
                        sections.put(section.substring(4), sectionValues);
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
            if (sectionValues != null && section.startsWith("ore.")) {
                sections.put(section.substring(4), sectionValues);
            }

            OreConfig.setValue("remove_vanilla_ore_generation", bool(top, "remove_vanilla_ore_generation", false));
            OreConfig.setValue("enable_mod_compat", bool(top, "enable_mod_compat", false));
            OreConfig.setValue("enable_mod_compat_2", bool(top, "enable_mod_compat_2", true));
            OreConfig.setValue("mod_compat_2_ores_in_end", bool(top, "mod_compat_2_ores_in_end", true));
            OreConfig.setValue("enable_vanilla_end_ores", bool(top, "enable_vanilla_end_ores", true));
            OreConfig.setValue("bad_luck_eliminator", bool(top, "bad_luck_eliminator", true));
            OreConfig.setValue("bad_luck_multiplier", decimal(top, "bad_luck_multiplier", 2.0D));
            OreConfig.setAdditionalOres(strList(top, "additional_ores", List.of()));
            for (Map.Entry<String, Map<String, String>> entry : sections.entrySet()) {
                OreConfig.applyOverrides(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            LOGGER.warn("[Ore Yield] Failed to read config file {}: {}", configFile, e.getMessage());
        }
        OreConfig.rebuild();
    }

    public static void save(Path configFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Ore Yield configuration\n");
        sb.append("# Set by the in-game config screen or edit manually, then use \"Reload from File\".\n\n");
        line(sb, "remove_vanilla_ore_generation", OreConfig.shouldRemoveVanillaOreGeneration());
        line(sb, "enable_mod_compat", OreConfig.isModCompatEnabled());
        line(sb, "enable_mod_compat_2", OreConfig.isModCompat2Enabled());
        line(sb, "mod_compat_2_ores_in_end", OreConfig.isModCompat2OresInEnd());
        line(sb, "enable_vanilla_end_ores", OreConfig.isVanillaEndOresEnabled());
        line(sb, "bad_luck_eliminator", OreConfig.isBadLuckEliminatorEnabled());
        line(sb, "bad_luck_multiplier", OreConfig.badLuckMultiplier());
        sb.append("\n");
        sb.append("additional_ores = ").append(toTomlList(OreConfig.getAdditionalOres())).append("\n");
        sb.append("# One entry per modded ore: id|enabled|result_item|min_count|max_count|chance|min_y|max_y|peak_y|fortune_type|xp_min|xp_max|dimension|host1,host2|min_pickaxe_level\n\n");

        for (OreEntry entry : OreConfig.defaultEntries().values()) {
            OreEntry effective = OreConfig.effectiveEntry(entry.id());
            sb.append("[ore.").append(effective.id()).append("]\n");
            line(sb, "enabled", effective.enabled());
            sb.append("host_blocks = ").append(toTomlList(effective.hosts())).append("\n");
            sb.append("result_item = \"").append(effective.resultItem()).append("\"\n");
            sb.append("min_count = ").append(effective.minCount()).append("\n");
            sb.append("max_count = ").append(effective.maxCount()).append("\n");
            sb.append("chance = ").append(effective.chance()).append("\n");
            sb.append("min_y = ").append(effective.minY()).append("\n");
            sb.append("max_y = ").append(effective.maxY()).append("\n");
            sb.append("peak_y = ").append(effective.peakY()).append("\n");
            sb.append("fortune_type = \"").append(effective.fortuneType().name()).append("\"\n");
            sb.append("xp_min = ").append(effective.xpMin()).append("\n");
            sb.append("xp_max = ").append(effective.xpMax()).append("\n");
            sb.append("dimension = \"").append(effective.dimension()).append("\"\n");
            sb.append("min_pickaxe_level = ").append(effective.minPickaxeLevel()).append("\n\n");
        }

        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, sb.toString());
        } catch (IOException e) {
            LOGGER.warn("[Ore Yield] Failed to write config file {}: {}", configFile, e.getMessage());
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
}
