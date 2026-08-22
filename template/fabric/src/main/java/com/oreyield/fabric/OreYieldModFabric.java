package com.oreyield.fabric;

import com.oreyield.OreYieldMod;
import com.oreyield.config.OreConfig;
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
import net.minecraft.world.level.block.Block;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//? if fabric_server_level_events {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
//?} else {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
//?}
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class OreYieldModFabric implements ModInitializer {
    private static void registerAntiCheeseContent() {
        var id = ResourceLocations.of(OreYieldMod.MOD_ID, "stone_generator");
        Block stoneGenerator = Registry.register(BuiltInRegistries.BLOCK, id,
                //? if block_properties_require_id {
                new StoneGeneratorBlock(id)
                //?} else {
                new StoneGeneratorBlock()
                //?}
                );
        for (ProvenanceHostBlocks.Definition definition : ProvenanceHostBlocks.definitions()) {
            var markerId = ResourceLocations.of(OreYieldMod.MOD_ID, definition.id());
            Registry.register(BuiltInRegistries.BLOCK, markerId,
                    //? if block_properties_require_id {
                    new ProvenanceHostBlock(definition.source(), definition.origin(), markerId)
                    //?} else {
                    new ProvenanceHostBlock(definition.source(), definition.origin())
                    //?}
                    );
        }
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(stoneGenerator,
                //? if block_properties_require_id {
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))
                //?} else {
                new Item.Properties()
                //?}
                ));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, StoneGeneratorRecipe.SERIALIZER);
    }

    @Override
    public void onInitialize() {
        OreYieldMod.init();
        if (OreConfig.isAntiCheeseMechanicsEnabled()) {
            registerAntiCheeseContent();
        }
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
