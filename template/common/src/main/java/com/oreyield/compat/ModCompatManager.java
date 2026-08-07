package com.oreyield.compat;

import com.oreyield.config.FortuneType;
import com.oreyield.config.OreEntry;
import com.oreyield.util.ResourceLocations;
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

public final class ModCompatManager {
    //? if tier_item_map {
    private static final TagKey<Block> ORE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("c", "ores"));
    private static final TagKey<Block> COBBLESTONE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("c", "cobblestones"));
    private static final TagKey<Block> STONE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("c", "stones"));
    //?} else {
    private static final TagKey<Block> ORE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("forge", "ores"));
    private static final TagKey<Block> COBBLESTONE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("forge", "cobblestone"));
    private static final TagKey<Block> STONE_TAG = TagKey.create(Registries.BLOCK, ResourceLocations.of("forge", "stone"));
    //?}

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

    private static final Set<String> ORE_EXCLUSIONS = Set.of(
            "redstone_ore", "lapis_ore" // vanilla handled separately
    );

    private static final Set<String> VANILLA_ORE_MATERIALS = Set.of(
            "coal", "iron", "copper", "gold", "redstone", "lapis",
            "diamond", "emerald", "quartz", "ancient_debris"
    );

    private static final Set<String> autoDetectedHosts = new HashSet<>();
    private static final List<OreEntry> autoDetectedOres = new ArrayList<>();
    private static final Set<String> moddedDimensionNamespaces = new HashSet<>();

    private ModCompatManager() {}

    /** Rebuilds the detected lists after registries and tags are available. */
    public static synchronized void scan() {
        autoDetectedHosts.clear();
        autoDetectedOres.clear();

        scanOres();
        scanStones();

        autoDetectedOres.sort(Comparator.comparing(OreEntry::id));

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/ModCompat");
        logger.info("[Ore Yield] Mod compat scan complete: {} ore entries, {} stone hosts detected",
                autoDetectedOres.size(), autoDetectedHosts.size());
    }

    public static void discoverModdedDimensions(net.minecraft.server.MinecraftServer server) {
        moddedDimensionNamespaces.clear();
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key : server.levelKeys()) {
            //? if resourcekey_identifier {
            String ns = key.identifier().getNamespace();
            //?} else {
            String ns = key.location().getNamespace();
            //?}
            if (!ns.equals("minecraft")) {
                moddedDimensionNamespaces.add(ns);
            }
        }
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/ModCompat");
        if (!moddedDimensionNamespaces.isEmpty()) {
            logger.info("[Ore Yield] Discovered modded dimension namespaces: {}", moddedDimensionNamespaces);
        }
        // The initial scan can occur before all server data is ready. Rebuild even
        // when no modded dimension was found so tags and registries are current.
        scan();
    }

    public static boolean hasModdedDimensions() {
        return !moddedDimensionNamespaces.isEmpty();
    }

    public static String inferDimensionForOre(ResourceLocation oreId) {
        // A mod namespace does not identify a dimension. Generic compatibility
        // entries use host/dimension rules rather than inventing one.
        return "";
    }

    private static void scanOres() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || id.getNamespace().equals("minecraft")) return;

            boolean isOre = block.builtInRegistryHolder().is(ORE_TAG)
                    || id.getPath().endsWith("_ore");
            if (!isOre) return;

            if (ORE_EXCLUSIONS.contains(id.getPath())) return;

            String material = id.getPath().endsWith("_ore")
                    ? id.getPath().substring(0, id.getPath().length() - "_ore".length())
                    : id.getPath();
            if (VANILLA_ORE_MATERIALS.contains(material)) return;

            String dropItem = findDropItem(id);
            if (!isRegisteredItem(dropItem)) return;
            String entryMaterial = id.getPath().endsWith("_ore")
                    ? id.getPath().substring(0, id.getPath().length() - "_ore".length())
                    : id.getPath();
            String entryId = id.getNamespace() + ":" + entryMaterial;

            OreEntry entry = new OreEntry(
                    entryId,
                    true,
                    List.of("#forge:ore_bearing_stones"),
                    dropItem,
                    1, 1,
                    0.05,
                    -64, 320,
                    -1,
                    FortuneType.ORE,
                    0, 0,
                    "",
                    0
            );
            autoDetectedOres.add(entry);
        });
    }

    private static void scanStones() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || id.getNamespace().equals("minecraft")) return;

            boolean isInCobbleTag = block.builtInRegistryHolder().is(COBBLESTONE_TAG);
            boolean isInStoneTag = block.builtInRegistryHolder().is(STONE_TAG);
            boolean isStoneByName = isStoneByName(id.getPath());

            boolean isOre = block.builtInRegistryHolder().is(ORE_TAG)
                    || id.getPath().endsWith("_ore");

            if ((isInCobbleTag || isInStoneTag || isStoneByName) && !isOre) {
                autoDetectedHosts.add(id.toString());
            }
        });
    }

    private static String findDropItem(ResourceLocation oreId) {
        String path = oreId.getPath();
        String material = path.endsWith("_ore") ? path.substring(0, path.length() - "_ore".length()) : path;

        ResourceLocation rawId = ResourceLocations.of(oreId.getNamespace(), "raw_" + material);
        if (BuiltInRegistries.ITEM.containsKey(rawId)) return rawId.toString();

        ResourceLocation rawId2 = ResourceLocations.of(oreId.getNamespace(), material + "_raw");
        if (BuiltInRegistries.ITEM.containsKey(rawId2)) return rawId2.toString();

        ResourceLocation ingotId = ResourceLocations.of(oreId.getNamespace(), material + "_ingot");
        if (BuiltInRegistries.ITEM.containsKey(ingotId)) return ingotId.toString();

        ResourceLocation gemId = ResourceLocations.of(oreId.getNamespace(), material + "_gem");
        if (BuiltInRegistries.ITEM.containsKey(gemId)) return gemId.toString();

        ResourceLocation materialId = ResourceLocations.of(oreId.getNamespace(), material);
        if (BuiltInRegistries.ITEM.containsKey(materialId)) return materialId.toString();

        return oreId.toString();
    }

    private static boolean isRegisteredItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        return id != null && BuiltInRegistries.ITEM.containsKey(id);
    }

    private static boolean isStoneByName(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (STONE_EXCLUSIONS.stream().anyMatch(lower::contains)) return false;
        return STONE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    public static boolean isStoneByNamePublic(String path) {
        return isStoneByName(path);
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
