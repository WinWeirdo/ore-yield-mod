package com.oreyield.fabric.worldgen;

import com.oreyield.config.OreConfig;
import com.oreyield.util.ResourceLocations;
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
     * Removes explicitly named vanilla mineral features and narrowly named modded ore
     * features.  The previous substring match removed unrelated generation such as
     * forest and terrain features whose identifiers happened to contain "ore".
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
            if (isModdedOreFeature(key)) {
                settings.removeFeature(key);
            }
        }
    }

    private static boolean isModdedOreFeature(ResourceKey<PlacedFeature> key) {
        //? if resourcekey_identifier {
        ResourceLocation id = key.identifier();
        //?} else {
        ResourceLocation id = key.location();
        //?}
        if (id.getNamespace().equals("minecraft")) return false;

        String path = id.getPath();
        return path.startsWith("ore_") || path.endsWith("_ore") || path.startsWith("ores/");
    }

    private static Registry<PlacedFeature> getPlacedFeatureRegistry(BiomeModificationContext context)
            throws ReflectiveOperationException {
        Field field = context.getClass().getDeclaredField("registries");
        field.setAccessible(true);
        RegistryAccess registries = (RegistryAccess) field.get(context);
        //? if registryaccess_lookup {
        return registries.lookupOrThrow(Registries.PLACED_FEATURE);
        //?} else {
        return registries.registryOrThrow(Registries.PLACED_FEATURE);
        //?}
    }
}
