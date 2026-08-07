package com.oreyield.neoforge;

import com.mojang.serialization.MapCodec;
import com.oreyield.OreYieldMod;
import com.oreyield.datagen.OreScannerProvider;
import com.oreyield.neoforge.loot.OreYieldLootModifier;
import com.oreyield.neoforge.worldgen.OreRemovalModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldModNeoForge {
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
        modBus.addListener(OreYieldModNeoForge::gatherData);
        NeoForge.EVENT_BUS.addListener(OreYieldModNeoForge::onServerStarting);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        OreYieldMod.discoverModdedDimensions(event.getServer());
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
