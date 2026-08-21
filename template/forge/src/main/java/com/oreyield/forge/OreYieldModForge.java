package com.oreyield.forge;

import com.mojang.serialization.Codec;
import com.oreyield.OreYieldMod;
import com.oreyield.block.StoneGeneratorBlock;
import com.oreyield.block.ProvenanceHostBlock;
import com.oreyield.block.ProvenanceHostBlocks;
import com.oreyield.recipe.StoneGeneratorRecipe;
import com.oreyield.datagen.OreScannerProvider;
import com.oreyield.forge.loot.OreYieldLootModifier;
import com.oreyield.forge.event.PlayerPlacedHostEvents;
import com.oreyield.forge.worldgen.OreRemovalModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldModForge {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OreYieldMod.MOD_ID);
    public static final RegistryObject<Block> STONE_GENERATOR = BLOCKS.register("stone_generator", StoneGeneratorBlock::new);
    public static final Map<String, RegistryObject<Block>> PROVENANCE_HOSTS = registerProvenanceHosts();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OreYieldMod.MOD_ID);
    public static final RegistryObject<Item> STONE_GENERATOR_ITEM = ITEMS.register("stone_generator",
            () -> new BlockItem(STONE_GENERATOR.get(), new Item.Properties()));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OreYieldMod.MOD_ID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<StoneGeneratorRecipe>> STONE_GENERATOR_RECIPE =
            RECIPE_SERIALIZERS.register("stone_generator", () -> StoneGeneratorRecipe.SERIALIZER);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OreYieldMod.MOD_ID);
    public static final RegistryObject<Codec<OreYieldLootModifier>> ORE_YIELD =
            LOOT_MODIFIERS.register("ore_yield", () -> OreYieldLootModifier.CODEC);

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, OreYieldMod.MOD_ID);
    public static final RegistryObject<Codec<OreRemovalModifier>> ORE_REMOVAL =
            BIOME_MODIFIERS.register("ore_removal", () -> OreRemovalModifier.CODEC);

    public OreYieldModForge() {
        OreYieldMod.init();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOOT_MODIFIERS.register(modBus);
        BIOME_MODIFIERS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(OreYieldModForge::gatherData);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(OreYieldModForge::onServerStarted);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PlayerPlacedHostEvents::onPlace);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        OreYieldMod.discoverModdedDimensions(event.getServer());
    }

    private static Map<String, RegistryObject<Block>> registerProvenanceHosts() {
        Map<String, RegistryObject<Block>> blocks = new LinkedHashMap<>();
        for (ProvenanceHostBlocks.Definition definition : ProvenanceHostBlocks.definitions()) {
            blocks.put(definition.id(), BLOCKS.register(definition.id(),
                    () -> new ProvenanceHostBlock(definition.source(), definition.origin())));
        }
        return Map.copyOf(blocks);
    }

    private static void gatherData(net.minecraftforge.data.event.GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new OreScannerProvider(event.getGenerator().getPackOutput()));
    }
}
