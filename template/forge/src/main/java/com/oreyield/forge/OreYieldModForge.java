package com.oreyield.forge;

import com.mojang.serialization.Codec;
import com.oreyield.OreYieldMod;
import com.oreyield.config.OreConfig;
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

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldModForge {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OreYieldMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OreYieldMod.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OreYieldMod.MOD_ID);
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
        if (OreConfig.isAntiCheeseMechanicsEnabled()) {
            registerAntiCheeseContent();
        }
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

    private static void registerAntiCheeseContent() {
        RegistryObject<Block> stoneGenerator = BLOCKS.register("stone_generator", StoneGeneratorBlock::new);
        for (ProvenanceHostBlocks.Definition definition : ProvenanceHostBlocks.definitions()) {
            BLOCKS.register(definition.id(), () -> new ProvenanceHostBlock(definition.source(), definition.origin()));
        }
        ITEMS.register("stone_generator", () -> new BlockItem(stoneGenerator.get(), new Item.Properties()));
        RECIPE_SERIALIZERS.register("stone_generator", () -> StoneGeneratorRecipe.SERIALIZER);
    }

    private static void gatherData(net.minecraftforge.data.event.GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new OreScannerProvider(event.getGenerator().getPackOutput()));
    }
}
