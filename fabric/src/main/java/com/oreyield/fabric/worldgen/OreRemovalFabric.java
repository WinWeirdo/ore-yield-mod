package com.oreyield.fabric.worldgen;

import com.oreyield.config.OreConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModification;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

public final class OreRemovalFabric {
    private static final List<ResourceLocation> FEATURE_IDS = List.of(
            new ResourceLocation("minecraft", "ore_coal_upper"),
            new ResourceLocation("minecraft", "ore_coal_lower"),
            new ResourceLocation("minecraft", "ore_iron_upper"),
            new ResourceLocation("minecraft", "ore_iron_middle"),
            new ResourceLocation("minecraft", "ore_iron_small"),
            new ResourceLocation("minecraft", "ore_copper"),
            new ResourceLocation("minecraft", "ore_copper_large"),
            new ResourceLocation("minecraft", "ore_gold"),
            new ResourceLocation("minecraft", "ore_gold_lower"),
            new ResourceLocation("minecraft", "ore_redstone"),
            new ResourceLocation("minecraft", "ore_redstone_lower"),
            new ResourceLocation("minecraft", "ore_lapis"),
            new ResourceLocation("minecraft", "ore_lapis_buried"),
            new ResourceLocation("minecraft", "ore_diamond"),
            new ResourceLocation("minecraft", "ore_diamond_large"),
            new ResourceLocation("minecraft", "ore_diamond_buried"),
            new ResourceLocation("minecraft", "ore_emerald"),
            new ResourceLocation("minecraft", "ore_quartz_nether"),
            new ResourceLocation("minecraft", "ore_gold_nether"),
            new ResourceLocation("minecraft", "ore_debris_large"),
            new ResourceLocation("minecraft", "ore_debris_small")
    );

    private OreRemovalFabric() {}

    public static void register() {
        BiomeModifications.create(new ResourceLocation("ore_yield", "remove_vanilla_ores"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (BiomeModificationContext context) -> {
                    if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;
                    BiomeModificationContext.GenerationSettingsContext settings = context.getGenerationSettings();
                    for (ResourceLocation id : FEATURE_IDS) {
                        settings.removeFeature(ResourceKey.create(Registries.PLACED_FEATURE, id));
                    }
                });
    }
}
