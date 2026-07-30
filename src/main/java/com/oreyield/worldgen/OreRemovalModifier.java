package com.oreyield.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oreyield.config.OreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

import java.util.List;

public record OreRemovalModifier(List<ResourceLocation> featureIds) implements BiomeModifier {

    public static final Codec<OreRemovalModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.listOf().fieldOf("features").forGetter(OreRemovalModifier::featureIds)
            ).apply(instance, OreRemovalModifier::new));

    @Override
    public void modify(net.minecraft.core.Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) return;
        if (!OreConfig.shouldRemoveVanillaOreGeneration()) return;

        BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            generationSettings.getFeatures(step).removeIf(holder ->
                    holder.unwrapKey().map(id -> featureIds.contains(id)).orElse(false));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
