package com.oreyield.fabric.worldgen;

import com.oreyield.config.OreConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModification;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

public final class OreRemovalFabric {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/WorldGen");
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
            new ResourceLocation("minecraft", "ore_ancient_debris_large"),
            new ResourceLocation("minecraft", "ore_ancient_debris_small")
    );

    private OreRemovalFabric() {}

    public static void register() {
        BiomeModifications.create(new ResourceLocation("ore_yield", "remove_vanilla_ores"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.all(), (BiomeModificationContext context) -> {
                    if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;
                    BiomeModificationContext.GenerationSettingsContext settings = context.getGenerationSettings();
                    for (ResourceLocation id : FEATURE_IDS) {
                        try {
                            settings.removeFeature(ResourceKey.create(Registries.PLACED_FEATURE, id));
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("[Ore Yield] Skipping feature removal for {}: not present in the placed feature registry.", id, e);
                        }
                    }
                    removeOreFeatures(settings, context);
                });
    }

    /**
     * Mirrors the Forge modifier: remove every placed feature whose id path contains "ore",
     * so modded ores (e.g. mod_compat_2 ores) stop generating alongside the vanilla list.
     * Fabric's API only removes features by explicit key, so the placed feature registry is
     * read from the modification context via reflection. Falls back to the vanilla list only
     * if the internal API changes.
     */
    private static void removeOreFeatures(BiomeModificationContext.GenerationSettingsContext settings,
                                          BiomeModificationContext context) {
        Registry<PlacedFeature> registry;
        try {
            registry = getPlacedFeatureRegistry(context);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[Ore Yield] Could not enumerate placed features for modded ore removal ({}); only the vanilla list is removed.", e.getMessage());
            return;
        }
        for (ResourceKey<PlacedFeature> key : registry.registryKeySet()) {
            if (key.location().getPath().contains("ore")) {
                settings.removeFeature(key);
            }
        }
    }

    private static Registry<PlacedFeature> getPlacedFeatureRegistry(BiomeModificationContext context)
            throws ReflectiveOperationException {
        Field field = context.getClass().getDeclaredField("registries");
        field.setAccessible(true);
        RegistryAccess registries = (RegistryAccess) field.get(context);
        return registries.registryOrThrow(Registries.PLACED_FEATURE);
    }
}
