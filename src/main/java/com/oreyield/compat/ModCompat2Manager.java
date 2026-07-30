package com.oreyield.compat;

import com.oreyield.config.FortuneType;
import com.oreyield.config.OreEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ModCompat2Manager {
    private static final TagKey<Block> FORGE_ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static final TagKey<Block> FORGE_COBBLESTONE = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "cobblestone"));
    private static final TagKey<Block> FORGE_STONE = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "stone"));

    private static final Set<String> STONE_KEYWORDS = Set.of(
            "stone", "marble", "limestone", "slate", "granite", "diorite", "andesite",
            "basalt", "tuff", "calcite", "rock", "chalk", "shale", "schist", "gneiss",
            "quartzite", "travertine", "serpentine", "mudstone", "siltstone",
            "breccia", "conglomerate", "graywacke"
    );

    private static final Set<String> STONE_EXCLUSIONS = Set.of(
            "sandstone", "redstone", "mossy", "infested", "brick",
            "end_stone", "obsidian", "crying_obsidian"
    );

    private static final String OW_DIM = "minecraft:overworld";
    private static final String NE_DIM = "minecraft:the_nether";
    private static final String EN_DIM = "minecraft:the_end";
    private static final String OW_HOSTS = "#forge:overworld_ore_bearing_stones";
    private static final String NE_HOSTS = "#forge:nether_ore_bearing_stones";
    private static final String EN_HOSTS = "minecraft:end_stone";

    private record OreSpec(String oreBlockId, String dropItem, int minPickaxeLevel,
                           String dimension, String hostTag,
                           int minCount, int maxCount, double chance,
                           int minY, int maxY, int peakY,
                           FortuneType fortuneType) {}

    private static final List<OreSpec> ORE_SPECS = List.of(
            // Overworld
            new OreSpec("iceandfire:silver_ore", "iceandfire:raw_silver", 1,
                    OW_DIM, OW_HOSTS, 1, 1, 0.02, -16, 112, -1, FortuneType.ORE),
            new OreSpec("simpleores:adamantium_ore", "simpleores:raw_adamantium", 2,
                    OW_DIM, OW_HOSTS, 1, 1, 0.0234375, 1, 84, -1, FortuneType.ORE),
            new OreSpec("simpleores:tin_ore", "simpleores:raw_tin", 1,
                    OW_DIM, OW_HOSTS, 2, 5, 0.037, 24, 236, 172, FortuneType.ORE),
            new OreSpec("simpleores:mythril_ore", "simpleores:raw_mythril", 2,
                    OW_DIM, OW_HOSTS, 1, 1, 0.03125, 1, 96, -1, FortuneType.ORE),
            new OreSpec("better_tools:ruby_ore", "better_tools:ruby", 1,
                    OW_DIM, OW_HOSTS, 2, 5, 0.0390625, -64, 16, -24, FortuneType.ORE),
            new OreSpec("better_tools:sapphire_ore", "better_tools:sapphire", 2,
                    OW_DIM, OW_HOSTS, 1, 1, 0.0390625, 100, 260, 180, FortuneType.ORE),
            new OreSpec("better_tools:topaz_ore", "better_tools:topaz", 2,
                    OW_DIM, OW_HOSTS, 2, 5, 0.014, 0, 120, 60, FortuneType.ORE),
            // Nether
            new OreSpec("simpleores:onyx_ore", "simpleores:onyx_gem", 3,
                    NE_DIM, NE_HOSTS, 1, 1, 0.01, 138, 246, -1, FortuneType.ORE),
            new OreSpec("better_tools:nether_diamond_ore", "better_tools:nether_diamond", 2,
                    NE_DIM, NE_HOSTS, 1, 1, 0.025, 128, 256, -1, FortuneType.ORE),
            new OreSpec("tconstruct:cobalt_ore", "tconstruct:raw_cobalt", 2,
                    NE_DIM, NE_HOSTS, 1, 1, 0.0115, 136, 248, 192, FortuneType.ORE),
            new OreSpec("netherrocks:argonite_ore", "netherrocks:raw_argonite", 3,
                    NE_DIM, NE_HOSTS, 1, 1, 0.015, 132, 252, -1, FortuneType.ORE),
            new OreSpec("netherrocks:ashstone_ore", "netherrocks:ashstone_gem", 3,
                    NE_DIM, NE_HOSTS, 1, 1, 0.015, 132, 252, -1, FortuneType.ORE),
            new OreSpec("netherrocks:dragonstone_ore", "netherrocks:dragonstone_gem", 3,
                    NE_DIM, NE_HOSTS, 1, 1, 0.0125, 129, 255, -1, FortuneType.ORE),
            new OreSpec("netherrocks:fyrite_ore", "netherrocks:raw_fyrite", 2,
                    NE_DIM, NE_HOSTS, 1, 1, 0.015, 129, 252, -1, FortuneType.ORE),
            new OreSpec("netherrocks:illumenite_ore", "netherrocks:raw_illumenite", 2,
                    NE_DIM, NE_HOSTS, 1, 1, 0.015, 128, 256, -1, FortuneType.ORE),
            new OreSpec("netherrocks:malachite_ore", "netherrocks:raw_malachite", 2,
                    NE_DIM, NE_HOSTS, 1, 1, 0.015, 138, 246, -1, FortuneType.ORE),
            // End — no Y restriction, void starts at 0 so forcing specific Y is dangerous
            // end_titanium_ore drops itself, so fortune should not multiply it
            new OreSpec("better_tools:end_titanium_ore", "better_tools:end_titanium_ore", 3,
                    EN_DIM, EN_HOSTS, 1, 1, 0.008, 0, 320, -1, FortuneType.NONE)
    );

    private static final Set<String> ORE_IDS = new HashSet<>();
    private static final Set<String> autoDetectedHosts = new HashSet<>();
    private static final List<OreEntry> autoDetectedOres = new ArrayList<>();
    private static final Set<String> moddedDimensionNamespaces = new HashSet<>();
    private static boolean scanned = false;

    static {
        for (OreSpec spec : ORE_SPECS) {
            ORE_IDS.add(spec.oreBlockId);
        }
    }

    private ModCompat2Manager() {}

    public static void scan() {
        if (scanned) return;
        scanned = true;
        autoDetectedHosts.clear();
        autoDetectedOres.clear();

        scanOres();
        scanStones();

        autoDetectedOres.sort(Comparator.comparing(OreEntry::id));

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/ModCompat2");
        logger.info("[Ore Yield] Mod compat 2 scan complete: {} ore entries, {} stone hosts detected",
                autoDetectedOres.size(), autoDetectedHosts.size());
    }

    public static void discoverModdedDimensions(net.minecraft.server.MinecraftServer server) {
        moddedDimensionNamespaces.clear();
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key : server.levelKeys()) {
            String ns = key.location().getNamespace();
            if (!ns.equals("minecraft")) {
                moddedDimensionNamespaces.add(ns);
            }
        }
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/ModCompat2");
        if (!moddedDimensionNamespaces.isEmpty()) {
            logger.info("[Ore Yield] Mod compat 2: Discovered modded dimension namespaces: {}", moddedDimensionNamespaces);
            if (scanned) {
                logger.info("[Ore Yield] Mod compat 2: Re-scanning ores with dimension info...");
                scanned = false;
                scan();
            }
        }
    }

    public static boolean hasModdedDimensions() {
        return !moddedDimensionNamespaces.isEmpty();
    }

    public static String inferDimensionForOre(ResourceLocation oreId) {
        String ns = oreId.getNamespace();
        if (ns.equals("minecraft")) return "";
        if (moddedDimensionNamespaces.contains(ns)) {
            return ns + ":" + oreId.getPath().replace("_ore", "");
        }
        return "";
    }

    private static void scanOres() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) return;

            String blockId = id.toString();
            if (!ORE_IDS.contains(blockId)) return;

            OreSpec spec = ORE_SPECS.stream().filter(s -> s.oreBlockId.equals(blockId)).findFirst().orElse(null);
            if (spec == null) return;

            String entryId = id.getNamespace() + ":" + id.getPath().replace("_ore", "");

            OreEntry entry = new OreEntry(
                    entryId,
                    true,
                    List.of(spec.hostTag),
                    spec.dropItem,
                    spec.minCount, spec.maxCount,
                    spec.chance,
                    spec.minY, spec.maxY,
                    spec.peakY,
                    spec.fortuneType,
                    0, 0,
                    spec.dimension,
                    spec.minPickaxeLevel
            );
            autoDetectedOres.add(entry);
        });
    }

    private static void scanStones() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || id.getNamespace().equals("minecraft")) return;

            boolean isInCobbleTag = block.builtInRegistryHolder().is(FORGE_COBBLESTONE);
            boolean isInStoneTag = block.builtInRegistryHolder().is(FORGE_STONE);
            boolean isStoneByName = isStoneByName(id.getPath());

            boolean isOre = block.builtInRegistryHolder().is(FORGE_ORES)
                    || id.getPath().endsWith("_ore");

            if ((isInCobbleTag || isInStoneTag || isStoneByName) && !isOre) {
                autoDetectedHosts.add(id.toString());
            }
        });
    }

    private static boolean isStoneByName(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (STONE_EXCLUSIONS.stream().anyMatch(lower::contains)) return false;
        return STONE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    public static boolean isAutoDetectedHost(String blockId) {
        return autoDetectedHosts.contains(blockId);
    }

    public static Set<String> getAutoDetectedHosts() {
        return Set.copyOf(autoDetectedHosts);
    }

    public static List<OreEntry> getAutoDetectedEntries() {
        return List.copyOf(autoDetectedOres);
    }

    public static List<String> getDetectedOreIds() {
        return autoDetectedOres.stream().map(OreEntry::id).toList();
    }

    public static List<String> getDetectedStoneIds() {
        return autoDetectedHosts.stream().sorted().toList();
    }
}
