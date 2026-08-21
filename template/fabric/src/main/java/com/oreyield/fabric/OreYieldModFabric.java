package com.oreyield.fabric;

import com.oreyield.OreYieldMod;
import com.oreyield.block.StoneGeneratorBlock;
import com.oreyield.block.ProvenanceHostBlock;
import com.oreyield.block.ProvenanceHostBlocks;
import com.oreyield.recipe.StoneGeneratorRecipe;
import com.oreyield.util.ResourceLocations;
import com.oreyield.fabric.event.BreakHandlerFabric;
import com.oreyield.fabric.worldgen.OreRemovalFabric;
import com.oreyield.loot.BreakRollStore;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//? if block_properties_require_id {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//? if fabric_server_level_events {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
//?} else {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
//?}
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class OreYieldModFabric implements ModInitializer {
    public static final Block STONE_GENERATOR = Registry.register(BuiltInRegistries.BLOCK,
            ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator"),
            //? if block_properties_require_id {
            new StoneGeneratorBlock(ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator"))
            //?} else {
            new StoneGeneratorBlock()
            //?}
            );
    public static final Map<String, Block> PROVENANCE_HOSTS = registerProvenanceHosts();
    public static final Item STONE_GENERATOR_ITEM = Registry.register(BuiltInRegistries.ITEM,
            ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator"), new BlockItem(STONE_GENERATOR,
            //? if block_properties_require_id {
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                    ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator")))
            //?} else {
            new Item.Properties()
            //?}
            ));
    public static final RecipeSerializer<StoneGeneratorRecipe> STONE_GENERATOR_RECIPE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator"), StoneGeneratorRecipe.SERIALIZER);

    private static Map<String, Block> registerProvenanceHosts() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (ProvenanceHostBlocks.Definition definition : ProvenanceHostBlocks.definitions()) {
            var id = ResourceLocations.of(OreYieldMod.MOD_ID, definition.id());
            blocks.put(definition.id(), Registry.register(BuiltInRegistries.BLOCK,
                    id,
                    //? if block_properties_require_id {
                    new ProvenanceHostBlock(definition.source(), definition.origin(), id)
                    //?} else {
                    new ProvenanceHostBlock(definition.source(), definition.origin())
                    //?}
                    ));
        }
        return Map.copyOf(blocks);
    }

    @Override
    public void onInitialize() {
        OreYieldMod.init();
        // AFTER is only fired for a completed break; rewards must never be paid for a
        // break that another mod or a protection plugin cancels in BEFORE.
        PlayerBlockBreakEvents.AFTER.register(BreakHandlerFabric::onAfterBreak);
        ServerLifecycleEvents.SERVER_STARTED.register(OreYieldMod::discoverModdedDimensions);
        //? if fabric_server_level_events {
        ServerLevelEvents.UNLOAD.register((server, world) -> BreakRollStore.onLevelUnload(world));
        //?} else {
        ServerWorldEvents.UNLOAD.register((server, world) -> BreakRollStore.onLevelUnload(world));
        //?}
        OreRemovalFabric.register();
    }
}
