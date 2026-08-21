package com.oreyield.fabric.worldgen;

import com.oreyield.config.OreConfig;
import com.oreyield.compat.CompatibleOreWorldgen;
import com.oreyield.util.ResourceLocations;
import net.fabricmc.fabric.api.biome.v1.BiomeModification;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class OreRemovalFabric {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/WorldGen");
    private static final List<ResourceLocation> VANILLA_FEATURE_IDS = List.of(
            ResourceLocations.of("minecraft", "ore_coal_upper"),
            ResourceLocations.of("minecraft", "ore_coal_lower"),
            ResourceLocations.of("minecraft", "ore_iron_upper"),
            ResourceLocations.of("minecraft", "ore_iron_middle"),
            ResourceLocations.of("minecraft", "ore_iron_small"),
            ResourceLocations.of("minecraft", "ore_copper"),
            ResourceLocations.of("minecraft", "ore_copper_large"),
            ResourceLocations.of("minecraft", "ore_gold"),
            ResourceLocations.of("minecraft", "ore_gold_extra"),
            ResourceLocations.of("minecraft", "ore_gold_lower"),
            ResourceLocations.of("minecraft", "ore_gold_deltas"),
            ResourceLocations.of("minecraft", "ore_redstone"),
            ResourceLocations.of("minecraft", "ore_redstone_lower"),
            ResourceLocations.of("minecraft", "ore_lapis"),
            ResourceLocations.of("minecraft", "ore_lapis_buried"),
            ResourceLocations.of("minecraft", "ore_diamond"),
            ResourceLocations.of("minecraft", "ore_diamond_medium"),
            ResourceLocations.of("minecraft", "ore_diamond_large"),
            ResourceLocations.of("minecraft", "ore_diamond_buried"),
            ResourceLocations.of("minecraft", "ore_emerald"),
            ResourceLocations.of("minecraft", "ore_quartz_nether"),
            ResourceLocations.of("minecraft", "ore_quartz_deltas"),
            ResourceLocations.of("minecraft", "ore_gold_nether"),
            ResourceLocations.of("minecraft", "ore_ancient_debris_large"),
            ResourceLocations.of("minecraft", "ore_debris_small")
    );

    private OreRemovalFabric() {}

    public static void register() {
        BiomeModifications.create(ResourceLocations.of("ore_yield", "remove_vanilla_ores"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (BiomeModificationContext context) -> {
                    BiomeModificationContext.GenerationSettingsContext settings = context.getGenerationSettings();
                    if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;
                    removeFeatures(settings, VANILLA_FEATURE_IDS);
                    if (OreConfig.shouldRemoveCompatibleOreGeneration()) {
                        removeFeatures(settings, CompatibleOreWorldgen.featuresToRemove());
                    }
                });
    }

    private static void removeFeatures(BiomeModificationContext.GenerationSettingsContext settings,
                                       List<ResourceLocation> featureIds) {
        for (ResourceLocation id : featureIds) {
            try {
                settings.removeFeature(ResourceKey.create(Registries.PLACED_FEATURE, id));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("[Ore Yield] Skipping feature removal for {}: not present in the placed feature registry.", id, e);
            }
        }
    }
}
