package com.oreyield.neoforge;

import com.mojang.serialization.MapCodec;
import com.oreyield.OreYieldMod;
import com.oreyield.block.StoneGeneratorBlock;
import com.oreyield.block.ProvenanceHostBlock;
import com.oreyield.block.ProvenanceHostBlocks;
import com.oreyield.recipe.StoneGeneratorRecipe;
import com.oreyield.datagen.OreScannerProvider;
import com.oreyield.neoforge.loot.OreYieldLootModifier;
import com.oreyield.neoforge.event.PlayerPlacedHostEvents;
import com.oreyield.neoforge.worldgen.OreRemovalModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.registries.Registries;
//? if block_properties_require_id {
import net.minecraft.resources.ResourceKey;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldModNeoForge {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, OreYieldMod.MOD_ID);
    public static final DeferredHolder<Block, StoneGeneratorBlock> STONE_GENERATOR = BLOCKS.register("stone_generator",
            //? if block_properties_require_id {
            id -> new StoneGeneratorBlock(id)
            //?} else {
            StoneGeneratorBlock::new
            //?}
            );
    public static final Map<String, DeferredHolder<Block, ? extends Block>> PROVENANCE_HOSTS = registerProvenanceHosts();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OreYieldMod.MOD_ID);
    public static final DeferredHolder<Item, BlockItem> STONE_GENERATOR_ITEM = ITEMS.register("stone_generator",
            //? if block_properties_require_id {
            id -> new BlockItem(STONE_GENERATOR.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id)))
            //?} else {
            () -> new BlockItem(STONE_GENERATOR.get(), new Item.Properties())
            //?}
            );
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, OreYieldMod.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<StoneGeneratorRecipe>> STONE_GENERATOR_RECIPE =
            RECIPE_SERIALIZERS.register("stone_generator", () -> StoneGeneratorRecipe.SERIALIZER);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OreYieldMod.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<OreYieldLootModifier>> ORE_YIELD =
            LOOT_MODIFIERS.register("ore_yield", () -> OreYieldLootModifier.CODEC);

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, OreYieldMod.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<OreRemovalModifier>> ORE_REMOVAL =
            BIOME_MODIFIERS.register("ore_removal", () -> OreRemovalModifier.CODEC);

    public OreYieldModNeoForge(IEventBus modBus) {
        OreYieldMod.init();
        LOOT_MODIFIERS.register(modBus);
        BIOME_MODIFIERS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(OreYieldModNeoForge::gatherData);
        NeoForge.EVENT_BUS.addListener(OreYieldModNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(PlayerPlacedHostEvents::onPlace);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        OreYieldMod.discoverModdedDimensions(event.getServer());
    }

    private static Map<String, DeferredHolder<Block, ? extends Block>> registerProvenanceHosts() {
        Map<String, DeferredHolder<Block, ? extends Block>> blocks = new LinkedHashMap<>();
        for (ProvenanceHostBlocks.Definition definition : ProvenanceHostBlocks.definitions()) {
            blocks.put(definition.id(), BLOCKS.register(definition.id(),
                    //? if block_properties_require_id {
                    id -> new ProvenanceHostBlock(definition.source(), definition.origin(), id)
                    //?} else {
                    () -> new ProvenanceHostBlock(definition.source(), definition.origin())
                    //?}
                    ));
        }
        return Map.copyOf(blocks);
    }

    //? if neoforge_server_data {
    private static void gatherData(net.neoforged.neoforge.data.event.GatherDataEvent.Server event) {
        event.addProvider(new OreScannerProvider(event.getGenerator().getPackOutput()));
    }
    //?} else {
    private static void gatherData(net.neoforged.neoforge.data.event.GatherDataEvent event) {
        //? if gather_data_include_server {
        event.getGenerator().addProvider(event.includeServer(), new OreScannerProvider(event.getGenerator().getPackOutput()));
        //?} else {
        event.addProvider(new OreScannerProvider(event.getGenerator().getPackOutput()));
        //?}
    }
    //?}
}
