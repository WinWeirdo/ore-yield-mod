package com.oreyield.neoforge.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oreyield.config.OreConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record OreRemovalModifier(List<ResourceLocation> featureIds) implements BiomeModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/WorldGen");

    public static final MapCodec<OreRemovalModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(OreRemovalModifier::featureIds)
            ).apply(instance, OreRemovalModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) return;
        if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;

        String biomeKey = biome.unwrapKey().map(k -> {
            //? if resourcekey_identifier {
            return k.identifier().toString();
            //?} else {
            return k.location().toString();
            //?}
        }).orElse("unknown");
        int removed = 0;

        BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            var before = generationSettings.getFeatures(step).size();
            generationSettings.getFeatures(step).removeIf(holder ->
                    holder.unwrapKey().map(id -> {
                        //? if resourcekey_identifier {
                        return shouldRemoveFeature(id.identifier());
                        //?} else {
                        return shouldRemoveFeature(id.location());
                        //?}
                    }).orElse(false));
            removed += before - generationSettings.getFeatures(step).size();
        }

        if (removed > 0) {
            LOGGER.info("[Ore Yield] Removed {} ore features from biome {}", removed, biomeKey);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }

    private boolean shouldRemoveFeature(ResourceLocation featureId) {
        // The data file is the authoritative explicit list of vanilla mineral
        // features.  Do not remove every feature whose name happens to contain
        // "ore" (for example, terrain/decorator features from other mods).
        if (featureIds.contains(featureId)) return true;
        if ("minecraft".equals(featureId.getNamespace())) return false;

        // Modded configured features conventionally use an "ore" path segment.
        // Match only that segment, not arbitrary text such as "forest".
        String path = featureId.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.startsWith("ore_") || path.endsWith("_ore") || path.contains("_ore_");
    }
}
