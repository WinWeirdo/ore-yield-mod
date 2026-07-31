package com.oreyield.config;

import com.oreyield.compat.ModCompat2Manager;
import com.oreyield.compat.ModCompatManager;
import com.oreyield.loot.BadLuckEliminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OreConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/Config");

    private static volatile Path configPath;
    private static volatile boolean removeVanillaOreGeneration = false;
    private static volatile boolean enableModCompat = false;
    private static volatile boolean enableModCompat2 = true;
    private static volatile boolean modCompat2OresInEnd = true;
    private static volatile boolean enableVanillaEndOres = true;
    private static volatile boolean badLuckEliminator = true;
    private static volatile double badLuckMultiplier = 2.0D;
    private static volatile List<String> additionalOres = List.of();
    private static volatile List<OreEntry> entries = List.of();

    private static final Map<String, OreEntry> DEFAULT_ENTRIES = new LinkedHashMap<>();
    private static final Map<String, OreEntry> OVERRIDES = new LinkedHashMap<>();

    static {
        String ow = "#forge:overworld_ore_bearing_stones";
        String ne = "#forge:nether_ore_bearing_stones";
        String owDim = "minecraft:overworld";
        String neDim = "minecraft:the_nether";
        String enDim = "minecraft:the_end";
        String es = "minecraft:end_stone";
        // Overworld ores — Y ranges/peaks per wiki Ore_(feature), chances ~1.5× vanilla density
        // minPickaxeLevel: 0=wooden, 1=stone, 2=iron, 3=diamond (per minecraft.wiki/w/Ore)
        add("coal", new OreEntry("coal", true, List.of(ow), "minecraft:coal", 1, 1, 0.050, 0, 320, 45, FortuneType.ORE, 0, 2, owDim, 0));
        add("iron", new OreEntry("iron", true, List.of(ow), "minecraft:raw_iron", 1, 1, 0.024, -64, 320, 14, FortuneType.ORE, 0, 0, owDim, 1));
        add("copper", new OreEntry("copper", true, List.of(ow), "minecraft:raw_copper", 2, 5, 0.018, -16, 112, 43, FortuneType.ORE, 0, 0, owDim, 1));
        add("gold", new OreEntry("gold", true, List.of(ow), "minecraft:raw_gold", 1, 1, 0.006, -64, 32, -18, FortuneType.ORE, 0, 0, owDim, 2));
        add("redstone", new OreEntry("redstone", true, List.of(ow), "minecraft:redstone", 4, 5, 0.013, -64, 16, -59, FortuneType.REDSTONE, 1, 5, owDim, 2));
        add("lapis", new OreEntry("lapis", true, List.of(ow), "minecraft:lapis_lazuli", 4, 9, 0.011, -64, 64, -2, FortuneType.ORE, 2, 5, owDim, 1));
        add("diamond", new OreEntry("diamond", true, List.of(ow), "minecraft:diamond", 1, 1, 0.006, -64, 16, -59, FortuneType.ORE, 3, 7, owDim, 2));
        add("emerald", new OreEntry("emerald", true, List.of(ow), "minecraft:emerald", 1, 1, 0.003, -16, 320, 85, FortuneType.ORE, 3, 7, owDim, 2));
        // Nether ores
        add("nether_quartz", new OreEntry("nether_quartz", true, List.of(ne), "minecraft:quartz", 1, 1, 0.024, 10, 117, 114, FortuneType.ORE, 2, 5, neDim, 0));
        add("nether_gold", new OreEntry("nether_gold", true, List.of(ne), "minecraft:gold_nugget", 2, 6, 0.011, 10, 117, 16, FortuneType.ORE, 0, 0, neDim, 0));
        add("ancient_debris", new OreEntry("ancient_debris", true, List.of(ne), "minecraft:ancient_debris", 1, 1, 0.002, 8, 119, 16, FortuneType.NONE, 0, 0, neDim, 3));
        // End ores — no Y restriction, void starts at 0 so forcing specific Y is dangerous
        add("end_coal", new OreEntry("end_coal", true, List.of(es), "minecraft:coal", 1, 1, 0.050, 0, 320, -1, FortuneType.ORE, 0, 2, enDim, 0));
        add("end_iron", new OreEntry("end_iron", true, List.of(es), "minecraft:raw_iron", 1, 1, 0.024, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 1));
        add("end_copper", new OreEntry("end_copper", true, List.of(es), "minecraft:raw_copper", 2, 5, 0.018, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 1));
        add("end_gold", new OreEntry("end_gold", true, List.of(es), "minecraft:raw_gold", 1, 1, 0.006, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 2));
        add("end_redstone", new OreEntry("end_redstone", true, List.of(es), "minecraft:redstone", 4, 5, 0.013, 0, 320, -1, FortuneType.REDSTONE, 1, 5, enDim, 2));
        add("end_lapis", new OreEntry("end_lapis", true, List.of(es), "minecraft:lapis_lazuli", 4, 9, 0.011, 0, 320, -1, FortuneType.ORE, 2, 5, enDim, 1));
        add("end_diamond", new OreEntry("end_diamond", true, List.of(es), "minecraft:diamond", 1, 1, 0.006, 0, 320, -1, FortuneType.ORE, 3, 7, enDim, 2));
        add("end_emerald", new OreEntry("end_emerald", true, List.of(es), "minecraft:emerald", 1, 1, 0.003, 0, 320, -1, FortuneType.ORE, 3, 7, enDim, 2));
        add("end_nether_quartz", new OreEntry("end_nether_quartz", true, List.of(es), "minecraft:quartz", 1, 1, 0.024, 0, 320, -1, FortuneType.ORE, 2, 5, enDim, 0));
        add("end_nether_gold", new OreEntry("end_nether_gold", true, List.of(es), "minecraft:gold_nugget", 2, 6, 0.011, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 0));
    }

    private OreConfig() {}

    private static void add(String id, OreEntry entry) {
        DEFAULT_ENTRIES.put(id, entry);
    }

    public static List<OreEntry> entriesFor(net.minecraft.world.level.block.state.BlockState state, String dimension) {
        return entries.stream().filter(entry -> entry.matches(state, dimension)).toList();
    }

    public static List<OreEntry> allEntries() {
        return entries;
    }

    public static Map<String, OreEntry> defaultEntries() {
        return DEFAULT_ENTRIES;
    }

    public static OreEntry effectiveEntry(String id) {
        return OVERRIDES.getOrDefault(id, DEFAULT_ENTRIES.get(id));
    }

    /** Drops all previously loaded [ore.*] section overrides (called when (re)reading the config file). */
    public static void clearOverrides() {
        OVERRIDES.clear();
    }

    public static boolean shouldRemoveVanillaOreGeneration() {
        return removeVanillaOreGeneration;
    }

    public static boolean isModCompatEnabled() {
        return enableModCompat;
    }

    public static boolean isModCompat2Enabled() {
        return enableModCompat2;
    }

    public static boolean isModCompat2OresInEnd() {
        return modCompat2OresInEnd;
    }

    public static boolean isVanillaEndOresEnabled() {
        return enableVanillaEndOres;
    }

    public static boolean isBadLuckEliminatorEnabled() {
        return badLuckEliminator;
    }

    public static double badLuckMultiplier() {
        return badLuckMultiplier;
    }

    public static List<String> getAdditionalOres() {
        return additionalOres;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void setConfigPath(Path path) {
        configPath = path;
    }

    public static void rebuild() {
        List<OreEntry> loaded = new ArrayList<>();
        for (OreEntry base : DEFAULT_ENTRIES.values()) {
            OreEntry entry = OVERRIDES.getOrDefault(base.id(), base);
            if (!enableVanillaEndOres && entry.id().startsWith("end_")) continue;
            loaded.add(entry);
        }
        for (String raw : additionalOres) parseAdditional(raw).ifPresent(loaded::add);
        if (enableModCompat) {
            loaded.addAll(ModCompatManager.getAutoDetectedEntries());
        }
        if (enableModCompat2) {
            loaded.addAll(ModCompat2Manager.getAutoDetectedEntries());
        }
        entries = List.copyOf(loaded);
        BadLuckEliminator.clearCounters();
    }

    public static void setValue(String key, boolean value) {
        switch (key) {
            case "remove_vanilla_ore_generation" -> removeVanillaOreGeneration = value;
            case "enable_mod_compat" -> enableModCompat = value;
            case "enable_mod_compat_2" -> enableModCompat2 = value;
            case "mod_compat_2_ores_in_end" -> modCompat2OresInEnd = value;
            case "enable_vanilla_end_ores" -> enableVanillaEndOres = value;
            case "bad_luck_eliminator" -> badLuckEliminator = value;
            default -> LOGGER.warn("[Ore Yield] Unknown boolean config key: {}", key);
        }
    }

    public static void setValue(String key, double value) {
        if ("bad_luck_multiplier".equals(key)) {
            badLuckMultiplier = Math.max(1.0D, value);
        } else {
            LOGGER.warn("[Ore Yield] Unknown numeric config key: {}", key);
        }
    }

    public static void setAdditionalOres(List<String> value) {
        additionalOres = List.copyOf(value);
    }

    public static void saveAndRebuild() {
        if (configPath != null) {
            OreConfigIO.save(configPath);
        }
        rebuild();
    }

    public static void reloadFromFile() {
        if (configPath == null) return;
        OreConfigIO.load(configPath);
    }

    public static void applyOverrides(String id, Map<String, String> values) {
        OreEntry base = DEFAULT_ENTRIES.get(id);
        if (base == null) {
            LOGGER.warn("[Ore Yield] Unknown ore section [ore.{}] in config; ignored.", id);
            return;
        }
        try {
            boolean enabled = bool(values, "enabled", base.enabled());
            List<String> hosts = list(values, "host_blocks", base.hosts());
            String item = str(values, "result_item", base.resultItem());
            int minCount = integer(values, "min_count", base.minCount());
            int rawMax = integer(values, "max_count", base.maxCount());
            int maxCount = Math.max(minCount, rawMax);
            if (rawMax < minCount) {
                LOGGER.warn("[Ore Yield] Ore '{}' has max_count={} less than min_count={}; max_count clamped to {}.", id, rawMax, minCount, maxCount);
            }
            double chance = decimal(values, "chance", base.chance());
            int minY = integer(values, "min_y", base.minY());
            int rawMaxY = integer(values, "max_y", base.maxY());
            int maxY = Math.max(minY, rawMaxY);
            if (rawMaxY < minY) {
                LOGGER.warn("[Ore Yield] Ore '{}' has max_y={} less than min_y={}; max_y clamped to {}.", id, rawMaxY, minY, maxY);
            }
            int peakY = integer(values, "peak_y", base.peakY());
            FortuneType fortune = FortuneType.parse(str(values, "fortune_type", base.fortuneType().name()));
            int xpMin = integer(values, "xp_min", base.xpMin());
            int rawXpMax = integer(values, "xp_max", base.xpMax());
            int xpMax = Math.max(xpMin, rawXpMax);
            if (rawXpMax < xpMin) {
                LOGGER.warn("[Ore Yield] Ore '{}' has xp_max={} less than xp_min={}; xp_max clamped to {}.", id, rawXpMax, xpMin, xpMax);
            }
            String dimension = str(values, "dimension", base.dimension());
            int pickLevel = integer(values, "min_pickaxe_level", base.minPickaxeLevel());
            OVERRIDES.put(id, new OreEntry(id, enabled, hosts, item, minCount, maxCount, chance, minY, maxY, peakY,
                    fortune, xpMin, xpMax, dimension, pickLevel));
        } catch (NumberFormatException e) {
            LOGGER.warn("[Ore Yield] Skipping malformed override for ore '{}': {}", id, e.getMessage());
        }
    }

    private static boolean bool(Map<String, String> values, String key, boolean def) {
        String v = values.get(key);
        return v == null ? def : v.equalsIgnoreCase("true");
    }

    private static int integer(Map<String, String> values, String key, int def) {
        String v = values.get(key);
        return v == null ? def : Integer.parseInt(v);
    }

    private static double decimal(Map<String, String> values, String key, double def) {
        String v = values.get(key);
        return v == null ? def : Double.parseDouble(v);
    }

    private static String str(Map<String, String> values, String key, String def) {
        String v = values.get(key);
        if (v == null) return def;
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static List<String> list(Map<String, String> values, String key, List<String> def) {
        String v = values.get(key);
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

    private static Optional<OreEntry> parseAdditional(String raw) {
        String[] part = raw.split("\\|", -1);
        if (part.length == 13) {
            return parseAdditionalLegacy(part, 0);
        }
        if (part.length == 14) {
            // Could be legacy-with-pickaxe (no dimension, field[13] is pickaxe level)
            // or dimension-without-pickaxe (field[12] is dimension, field[13] is hosts)
            try {
                int pickLevel = Integer.parseInt(part[13].trim());
                return parseAdditionalLegacy(part, Math.max(0, Math.min(3, pickLevel)));
            } catch (NumberFormatException e) {
                return parseAdditionalWithDimension(part, 0);
            }
        }
        if (part.length == 15) {
            // dimension + min_pickaxe_level
            try {
                int pickLevel = Integer.parseInt(part[14].trim());
                return parseAdditionalWithDimension(part, Math.max(0, Math.min(3, pickLevel)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        LOGGER.warn("[Ore Yield] Skipping malformed additional_ore entry (expected 13-15 fields, got {}): {}", part.length, raw);
        return Optional.empty();
    }

    private static Optional<OreEntry> parseAdditionalLegacy(String[] part, int minPickaxeLevel) {
        try {
            int minCount = Integer.parseInt(part[3]);
            int maxCount = Math.max(minCount, Integer.parseInt(part[4]));
            int minY = Integer.parseInt(part[6]);
            int maxY = Math.max(minY, Integer.parseInt(part[7]));
            List<String> hosts = java.util.Arrays.stream(part[12].split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (part[0].isBlank() || part[2].isBlank() || hosts.isEmpty()) return Optional.empty();
            return Optional.of(new OreEntry(part[0], Boolean.parseBoolean(part[1]), hosts, part[2], minCount, maxCount,
                    Double.parseDouble(part[5]), minY, maxY, Integer.parseInt(part[8]), FortuneType.parse(part[9]),
                    Integer.parseInt(part[10]), Math.max(Integer.parseInt(part[10]), Integer.parseInt(part[11])), "",
                    minPickaxeLevel));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("[Ore Yield] Skipping malformed additional_ore entry: {}", String.join("|", part), e);
            return Optional.empty();
        }
    }

    private static Optional<OreEntry> parseAdditionalWithDimension(String[] part, int minPickaxeLevel) {
        try {
            int minCount = Integer.parseInt(part[3]);
            int maxCount = Math.max(minCount, Integer.parseInt(part[4]));
            int minY = Integer.parseInt(part[6]);
            int maxY = Math.max(minY, Integer.parseInt(part[7]));
            String dimension = part[12].trim();
            List<String> hosts = java.util.Arrays.stream(part[13].split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (part[0].isBlank() || part[2].isBlank() || hosts.isEmpty()) return Optional.empty();
            return Optional.of(new OreEntry(part[0], Boolean.parseBoolean(part[1]), hosts, part[2], minCount, maxCount,
                    Double.parseDouble(part[5]), minY, maxY, Integer.parseInt(part[8]), FortuneType.parse(part[9]),
                    Integer.parseInt(part[10]), Math.max(Integer.parseInt(part[10]), Integer.parseInt(part[11])), dimension,
                    minPickaxeLevel));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("[Ore Yield] Skipping malformed additional_ore entry: {}", String.join("|", part), e);
            return Optional.empty();
        }
    }
}
