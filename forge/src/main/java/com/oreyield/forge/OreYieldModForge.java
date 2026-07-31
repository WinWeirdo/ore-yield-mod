package com.oreyield.forge;

import com.mojang.serialization.Codec;
import com.oreyield.OreYieldMod;
import com.oreyield.datagen.OreScannerProvider;
import com.oreyield.forge.loot.OreYieldLootModifier;
import com.oreyield.forge.worldgen.OreRemovalModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldModForge {
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
        modBus.addListener(OreYieldModForge::gatherData);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(OreYieldModForge::onServerStarting);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        OreYieldMod.discoverModdedDimensions(event.getServer());
    }

    private static void gatherData(net.minecraftforge.data.event.GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new OreScannerProvider(event.getGenerator().getPackOutput()));
    }
}
