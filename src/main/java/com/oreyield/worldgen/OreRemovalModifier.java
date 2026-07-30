package com.oreyield.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oreyield.config.OreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record OreRemovalModifier(List<ResourceLocation> featureIds) implements BiomeModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("ore_yield/WorldGen");

    public static final Codec<OreRemovalModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(OreRemovalModifier::featureIds)
            ).apply(instance, OreRemovalModifier::new));

    @Override
    public void modify(net.minecraft.core.Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) return;
        if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;

        String biomeKey = biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        int removed = 0;

        BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            var before = generationSettings.getFeatures(step).size();
            generationSettings.getFeatures(step).removeIf(holder ->
                    holder.unwrapKey().map(id -> {
                        if (featureIds.contains(id.location())) return true;
                        String path = id.location().getPath().toLowerCase(java.util.Locale.ROOT);
                        return path.contains("ore");
                    }).orElse(false));
            removed += before - generationSettings.getFeatures(step).size();
        }

        if (removed > 0) {
            LOGGER.info("[Ore Yield] Removed {} ore features from biome {}", removed, biomeKey);
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
