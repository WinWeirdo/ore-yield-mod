package com.oreyield.config;

import com.oreyield.OreYieldMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OreConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Map<String, Values> VALUES = new LinkedHashMap<>();
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> additionalOres;
    private static ForgeConfigSpec.BooleanValue removeVanillaOreGeneration;
    private static ForgeConfigSpec.BooleanValue enableModCompat;
    private static ForgeConfigSpec.BooleanValue enableModCompat2;
    private static ForgeConfigSpec.BooleanValue modCompat2OresInEnd;
    private static ForgeConfigSpec.BooleanValue enableVanillaEndOres;
    public static final ForgeConfigSpec SPEC;
    private static volatile List<OreEntry> entries = List.of();
    private static ModConfig loadedConfig;

    static {
        removeVanillaOreGeneration = BUILDER
                .comment("When true, vanilla ore placed-features are removed from terrain generation.",
                        "When false (default), ores generate naturally AND stone blocks still yield extra ore drops.")
                .define("remove_vanilla_ore_generation", false);
        enableModCompat = BUILDER
                .comment("Experimental: when true, automatically detects modded ore blocks and stone-like blocks at startup.",
                        "Detected ores become available as configurable entries (disabled by default).",
                        "Detected stone blocks become valid hosts for ore drops.",
                        "Requires restart to take effect.")
                .define("enable_mod_compat", false);
        enableModCompat2 = BUILDER
                .comment("When true, enables a curated set of modded ores (iceandfire, simpleores, better_tools, tconstruct, netherrocks).",
                        "Only the specific ores listed in the allowlist are enabled.",
                        "Detected stone blocks become valid hosts for ore drops.",
                        "Requires restart to take effect.")
                .define("enable_mod_compat_2", true);
        modCompat2OresInEnd = BUILDER
                .comment("When true, mod_compat_2 ores configured for overworld/nether also drop in the End.",
                        "Ancient debris is always excluded from End drops.")
                .define("mod_compat_2_ores_in_end", true);
        enableVanillaEndOres = BUILDER
                .comment("When true (default), vanilla End ore drops (coal, iron, copper, gold, redstone, lapis,",
                        "diamond, emerald, quartz, gold nuggets) are active in the End dimension.",
                        "When false, all vanilla End ore drops are disabled.")
                .define("enable_vanilla_end_ores", true);
        BUILDER.push("ores");
        String ow = "#forge:overworld_ore_bearing_stones";
        String ne = "#forge:nether_ore_bearing_stones";
        String owDim = "minecraft:overworld";
        String neDim = "minecraft:the_nether";
        String enDim = "minecraft:the_end";
        // Overworld ores — Y ranges/peaks per wiki Ore_(feature), chances ~1.5× vanilla density
        // minPickaxeLevel: 0=wooden, 1=stone, 2=iron, 3=diamond (per minecraft.wiki/w/Ore)
        add("coal", "minecraft:coal", 1, 1, 0.050, 0, 320, 45, FortuneType.ORE, 0, 2, owDim, 0, ow);
        add("iron", "minecraft:raw_iron", 1, 1, 0.024, -64, 320, 14, FortuneType.ORE, 0, 0, owDim, 1, ow);
        add("copper", "minecraft:raw_copper", 2, 5, 0.018, -16, 112, 43, FortuneType.ORE, 0, 0, owDim, 1, ow);
        add("gold", "minecraft:raw_gold", 1, 1, 0.006, -64, 32, -18, FortuneType.ORE, 0, 0, owDim, 2, ow);
        add("redstone", "minecraft:redstone", 4, 5, 0.013, -64, 16, -59, FortuneType.REDSTONE, 1, 5, owDim, 2, ow);
        add("lapis", "minecraft:lapis_lazuli", 4, 9, 0.011, -64, 64, -2, FortuneType.ORE, 2, 5, owDim, 1, ow);
        add("diamond", "minecraft:diamond", 1, 1, 0.006, -64, 16, -59, FortuneType.ORE, 3, 7, owDim, 2, ow);
        add("emerald", "minecraft:emerald", 1, 1, 0.003, -16, 320, 85, FortuneType.ORE, 3, 7, owDim, 2, ow);
        // Nether ores
        add("nether_quartz", "minecraft:quartz", 1, 1, 0.024, 10, 117, 114, FortuneType.ORE, 2, 5, neDim, 0, ne);
        add("nether_gold", "minecraft:gold_nugget", 2, 6, 0.011, 10, 117, 16, FortuneType.ORE, 0, 0, neDim, 0, ne);
        add("ancient_debris", "minecraft:ancient_debris", 1, 1, 0.002, 8, 119, 16, FortuneType.NONE, 0, 0, neDim, 3, ne);
        String es = "minecraft:end_stone";
        // End ores — no Y restriction, void starts at 0 so forcing specific Y is dangerous
        add("end_coal", "minecraft:coal", 1, 1, 0.050, 0, 320, -1, FortuneType.ORE, 0, 2, enDim, 0, es);
        add("end_iron", "minecraft:raw_iron", 1, 1, 0.024, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 1, es);
        add("end_copper", "minecraft:raw_copper", 2, 5, 0.018, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 1, es);
        add("end_gold", "minecraft:raw_gold", 1, 1, 0.006, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 2, es);
        add("end_redstone", "minecraft:redstone", 4, 5, 0.013, 0, 320, -1, FortuneType.REDSTONE, 1, 5, enDim, 2, es);
        add("end_lapis", "minecraft:lapis_lazuli", 4, 9, 0.011, 0, 320, -1, FortuneType.ORE, 2, 5, enDim, 1, es);
        add("end_diamond", "minecraft:diamond", 1, 1, 0.006, 0, 320, -1, FortuneType.ORE, 3, 7, enDim, 2, es);
        add("end_emerald", "minecraft:emerald", 1, 1, 0.003, 0, 320, -1, FortuneType.ORE, 3, 7, enDim, 2, es);
        add("end_nether_quartz", "minecraft:quartz", 1, 1, 0.024, 0, 320, -1, FortuneType.ORE, 2, 5, enDim, 0, es);
        add("end_nether_gold", "minecraft:gold_nugget", 2, 6, 0.011, 0, 320, -1, FortuneType.ORE, 0, 0, enDim, 0, es);
        BUILDER.pop();
        additionalOres = BUILDER.comment("One entry per modded ore: id|enabled|result_item|min_count|max_count|chance|min_y|max_y|peak_y|fortune_type|xp_min|xp_max|dimension|host1,host2|min_pickaxe_level",
                        "dimension is optional: 'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end', or empty for any dimension.",
                        "min_pickaxe_level is optional (0-3, default 0): 0=wooden, 1=stone, 2=iron, 3=diamond.")
                .defineList("additional_ores", List.of(), value -> value instanceof String);
        SPEC = BUILDER.build();
    }

    private OreConfig() {}

    private static void add(String id, String item, int minCount, int maxCount, double chance, int minY, int maxY,
                            int peakY, FortuneType fortune, int xpMin, int xpMax, String dimension,
                            int minPickaxeLevel, String... hosts) {
        BUILDER.push(id);
        Values value = new Values(
                BUILDER.define("enabled", true),
                BUILDER.defineList("host_blocks", List.of(hosts), value1 -> value1 instanceof String),
                BUILDER.define("result_item", item),
                BUILDER.defineInRange("min_count", minCount, 1, 64),
                BUILDER.defineInRange("max_count", maxCount, 1, 64),
                BUILDER.defineInRange("chance", chance, 0.0D, 1.0D),
                BUILDER.defineInRange("min_y", minY, -1024, 3200),
                BUILDER.defineInRange("max_y", maxY, -1024, 3200),
                BUILDER.defineInRange("peak_y", peakY, -1, 3200),
                BUILDER.defineInList("fortune_type", fortune.name(), List.of("NONE", "ORE", "REDSTONE")),
                BUILDER.defineInRange("xp_min", xpMin, 0, 100),
                BUILDER.defineInRange("xp_max", xpMax, 0, 100),
                BUILDER.define("dimension", dimension),
                BUILDER.defineInRange("min_pickaxe_level", minPickaxeLevel, 0, 3));
        VALUES.put(id, value);
        BUILDER.pop();
    }

    public static List<OreEntry> entriesFor(net.minecraft.world.level.block.state.BlockState state, String dimension) {
        return entries.stream().filter(entry -> entry.matches(state, dimension)).toList();
    }

    public static List<OreEntry> allEntries() {
        return entries;
    }

    public static boolean shouldRemoveVanillaOreGeneration() {
        return removeVanillaOreGeneration.get();
    }

    public static boolean isModCompatEnabled() {
        return enableModCompat.get();
    }

    public static boolean isModCompat2Enabled() {
        return enableModCompat2.get();
    }

    public static boolean isModCompat2OresInEnd() {
        return modCompat2OresInEnd.get();
    }

    public static boolean isVanillaEndOresEnabled() {
        return enableVanillaEndOres.get();
    }

    public static void rebuild() {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/Config");
        List<OreEntry> loaded = new ArrayList<>();
        VALUES.forEach((id, value) -> {
            if (!enableVanillaEndOres.get() && id.startsWith("end_")) return;
            int min = value.minCount.get();
            int rawMax = value.maxCount.get();
            int max = Math.max(min, rawMax);
            if (rawMax < min) {
                logger.warn("[Ore Yield] Ore '{}' has max_count={} less than min_count={}; max_count clamped to {}.", id, rawMax, min, max);
            }
            int minY = value.minY.get();
            int rawMaxY = value.maxY.get();
            int maxY = Math.max(minY, rawMaxY);
            if (rawMaxY < minY) {
                logger.warn("[Ore Yield] Ore '{}' has max_y={} less than min_y={}; max_y clamped to {}.", id, rawMaxY, minY, maxY);
            }
            int peak = value.peakY.get();
            if (peak >= 0 && minY == maxY) {
                logger.warn("[Ore Yield] Ore '{}' has peak_y={} but min_y==max_y={}; peak weighting will be ignored. Set peak_y to -1 to disable.", id, peak, minY);
            }
            int xpMin = value.xpMin.get();
            int rawXpMax = value.xpMax.get();
            if (rawXpMax < xpMin) {
                logger.warn("[Ore Yield] Ore '{}' has xp_max={} less than xp_min={}; xp_max clamped to {}.", id, rawXpMax, xpMin, xpMin);
            }
            loaded.add(new OreEntry(id, value.enabled.get(), value.hosts.get().stream().map(Object::toString).toList(),
                    value.item.get(), min, max, value.chance.get(), minY, maxY, peak,
                    FortuneType.parse(value.fortuneType.get()), xpMin, Math.max(xpMin, rawXpMax),
                    value.dimension.get(), value.minPickaxeLevel.get()));
        });
        for (String raw : additionalOres.get()) parseAdditional(raw).ifPresent(loaded::add);
        if (enableModCompat.get()) {
            loaded.addAll(com.oreyield.compat.ModCompatManager.getAutoDetectedEntries());
        }
        if (enableModCompat2.get()) {
            loaded.addAll(com.oreyield.compat.ModCompat2Manager.getAutoDetectedEntries());
        }
        entries = List.copyOf(loaded);
    }

    public static void setLoadedConfig(ModConfig config) {
        loadedConfig = config;
    }

    public static void setValue(String key, boolean value) {
        switch (key) {
            case "remove_vanilla_ore_generation" -> removeVanillaOreGeneration.set(value);
            case "enable_mod_compat" -> enableModCompat.set(value);
            case "enable_mod_compat_2" -> enableModCompat2.set(value);
            case "mod_compat_2_ores_in_end" -> modCompat2OresInEnd.set(value);
            case "enable_vanilla_end_ores" -> enableVanillaEndOres.set(value);
        }
    }

    public static void saveAndRebuild() {
        if (loadedConfig != null) {
            loadedConfig.save();
        }
        rebuild();
    }

    public static void reloadFromFile() {
        if (loadedConfig == null) return;
        Path configPath = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("ore_yield-common.toml");
        try {
            String content = Files.readString(configPath);
            removeVanillaOreGeneration.set(parseBooleanValue(content, "remove_vanilla_ore_generation", false));
            enableModCompat.set(parseBooleanValue(content, "enable_mod_compat", false));
            enableModCompat2.set(parseBooleanValue(content, "enable_mod_compat_2", true));
            modCompat2OresInEnd.set(parseBooleanValue(content, "mod_compat_2_ores_in_end", true));
            enableVanillaEndOres.set(parseBooleanValue(content, "enable_vanilla_end_ores", true));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("ore_yield/Config")
                    .warn("[Ore Yield] Failed to reload config from file: {}", e.getMessage());
        }
        rebuild();
    }

    private static boolean parseBooleanValue(String content, String key, boolean defaultValue) {
        for (String line : content.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith(key) && trimmed.contains("=")) {
                String afterEq = trimmed.substring(trimmed.indexOf('=') + 1).strip();
                if (afterEq.startsWith("true")) return true;
                if (afterEq.startsWith("false")) return false;
            }
        }
        return defaultValue;
    }

    private static java.util.Optional<OreEntry> parseAdditional(String raw) {
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
                return java.util.Optional.empty();
            }
        }
        org.slf4j.LoggerFactory.getLogger("ore_yield/Config").warn("[Ore Yield] Skipping malformed additional_ore entry (expected 13-15 fields, got {}): {}", part.length, raw);
        return java.util.Optional.empty();
    }

    private static java.util.Optional<OreEntry> parseAdditionalLegacy(String[] part, int minPickaxeLevel) {
        try {
            int minCount = Integer.parseInt(part[3]);
            int maxCount = Math.max(minCount, Integer.parseInt(part[4]));
            int minY = Integer.parseInt(part[6]);
            int maxY = Math.max(minY, Integer.parseInt(part[7]));
            List<String> hosts = java.util.Arrays.stream(part[12].split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (part[0].isBlank() || part[2].isBlank() || hosts.isEmpty()) return java.util.Optional.empty();
            return java.util.Optional.of(new OreEntry(part[0], Boolean.parseBoolean(part[1]), hosts, part[2], minCount, maxCount,
                    Double.parseDouble(part[5]), minY, maxY, Integer.parseInt(part[8]), FortuneType.parse(part[9]),
                    Integer.parseInt(part[10]), Math.max(Integer.parseInt(part[10]), Integer.parseInt(part[11])), "",
                    minPickaxeLevel));
        } catch (IllegalArgumentException e) {
            org.slf4j.LoggerFactory.getLogger("ore_yield/Config").warn("[Ore Yield] Skipping malformed additional_ore entry: {}", String.join("|", part), e);
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<OreEntry> parseAdditionalWithDimension(String[] part, int minPickaxeLevel) {
        try {
            int minCount = Integer.parseInt(part[3]);
            int maxCount = Math.max(minCount, Integer.parseInt(part[4]));
            int minY = Integer.parseInt(part[6]);
            int maxY = Math.max(minY, Integer.parseInt(part[7]));
            String dimension = part[12].trim();
            List<String> hosts = java.util.Arrays.stream(part[13].split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (part[0].isBlank() || part[2].isBlank() || hosts.isEmpty()) return java.util.Optional.empty();
            return java.util.Optional.of(new OreEntry(part[0], Boolean.parseBoolean(part[1]), hosts, part[2], minCount, maxCount,
                    Double.parseDouble(part[5]), minY, maxY, Integer.parseInt(part[8]), FortuneType.parse(part[9]),
                    Integer.parseInt(part[10]), Math.max(Integer.parseInt(part[10]), Integer.parseInt(part[11])), dimension,
                    minPickaxeLevel));
        } catch (IllegalArgumentException e) {
            org.slf4j.LoggerFactory.getLogger("ore_yield/Config").warn("[Ore Yield] Skipping malformed additional_ore entry: {}", String.join("|", part), e);
            return java.util.Optional.empty();
        }
    }

    @Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent.Loading event) {
            if (event.getConfig().getSpec() == SPEC) {
                setLoadedConfig(event.getConfig());
                if (enableModCompat.get()) {
                    com.oreyield.compat.ModCompatManager.scan();
                    OreYieldMod.setModCompatActive(true);
                }
                if (enableModCompat2.get()) {
                    com.oreyield.compat.ModCompat2Manager.scan();
                    OreYieldMod.setModCompat2Active(true);
                }
                rebuild();
            }
        }

        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == SPEC) rebuild();
        }
    }

    private record Values(ForgeConfigSpec.BooleanValue enabled, ForgeConfigSpec.ConfigValue<List<? extends String>> hosts,
                          ForgeConfigSpec.ConfigValue<String> item, ForgeConfigSpec.IntValue minCount,
                          ForgeConfigSpec.IntValue maxCount, ForgeConfigSpec.DoubleValue chance,
                          ForgeConfigSpec.IntValue minY, ForgeConfigSpec.IntValue maxY, ForgeConfigSpec.IntValue peakY,
                          ForgeConfigSpec.ConfigValue<String> fortuneType, ForgeConfigSpec.IntValue xpMin,
                          ForgeConfigSpec.IntValue xpMax, ForgeConfigSpec.ConfigValue<String> dimension,
                          ForgeConfigSpec.IntValue minPickaxeLevel) {}
}
