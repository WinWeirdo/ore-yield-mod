package com.oreyield.datagen;

import com.oreyield.compat.ModCompatManager;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/** Emits disabled TOML stubs for registered mod ore blocks and lists detected stone blocks during the Gradle data run. */
public final class OreScannerProvider implements DataProvider {
    private static final TagKey<Block> FORGE_ORES = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static final TagKey<Block> FORGE_COBBLESTONE = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("forge", "cobblestone"));
    private static final TagKey<Block> FORGE_STONE = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("forge", "stone"));

    private final PackOutput output;

    public OreScannerProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path target = output.getOutputFolder().resolve("ore-yield/generated_ores.json");
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();

        com.google.gson.JsonArray ores = new com.google.gson.JsonArray();
        BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> !entry.getKey().location().getNamespace().equals("minecraft"))
                .filter(entry -> entry.getKey().location().getPath().endsWith("_ore") || isInTag(entry.getValue(), FORGE_ORES))
                .map(entry -> entry.getKey().location())
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> ores.add(new com.google.gson.JsonPrimitive(stubOre(id))));
        root.add("additional_ores", ores);

        com.google.gson.JsonArray stones = new com.google.gson.JsonArray();
        BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> !entry.getKey().location().getNamespace().equals("minecraft"))
                .filter(entry -> !entry.getKey().location().getPath().endsWith("_ore"))
                .filter(entry -> isInTag(entry.getValue(), FORGE_COBBLESTONE) || isInTag(entry.getValue(), FORGE_STONE) || ModCompatManager.isStoneByNamePublic(entry.getKey().location().getPath()))
                .map(entry -> entry.getKey().location().toString())
                .sorted()
                .forEach(id -> stones.add(new com.google.gson.JsonPrimitive(id)));
        root.add("detected_stones", stones);

        return DataProvider.saveStable(cache, root, target);
    }

    @Override
    public String getName() {
        return "Ore Yield mod ore scanner";
    }

    private static String stubOre(ResourceLocation oreBlock) {
        String id = oreBlock.getNamespace() + "_" + oreBlock.getPath().replace('/', '_');
        return id + "|false|" + oreBlock + "|1|1|0.0|-64|320|-1|NONE|0|0||#forge:ore_bearing_stones|0";
    }

    private static boolean isInTag(Block block, TagKey<Block> tag) {
        for (net.minecraft.core.Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            if (holder.value() == block) return true;
        }
        return false;
    }
}
