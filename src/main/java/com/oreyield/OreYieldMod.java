package com.oreyield;

import com.mojang.serialization.Codec;
import com.oreyield.config.OreConfig;
import com.oreyield.datagen.OreScannerProvider;
import com.oreyield.loot.OreYieldLootModifier;
import com.oreyield.worldgen.OreRemovalModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(OreYieldMod.MOD_ID)
public final class OreYieldMod {
    public static final String MOD_ID = "ore_yield";
    private static volatile boolean modCompatActive = false;
    private static volatile boolean modCompatActive2 = false;
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<OreYieldLootModifier>> ORE_YIELD =
            LOOT_MODIFIERS.register("ore_yield", () -> OreYieldLootModifier.CODEC);

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<OreRemovalModifier>> ORE_REMOVAL =
            BIOME_MODIFIERS.register("ore_removal", () -> OreRemovalModifier.CODEC);

    public OreYieldMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOOT_MODIFIERS.register(modBus);
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(OreScannerProvider::gatherData);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, OreConfig.SPEC);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onServerStarting(ServerStartingEvent event) {
        if (isModCompatEnabled()) {
            com.oreyield.compat.ModCompatManager.discoverModdedDimensions(event.getServer());
        }
        if (isModCompat2Enabled()) {
            com.oreyield.compat.ModCompat2Manager.discoverModdedDimensions(event.getServer());
        }
        OreConfig.rebuild();
    }

    private static boolean isModCompatEnabled() {
        try {
            return OreConfig.isModCompatEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isModCompat2Enabled() {
        try {
            return OreConfig.isModCompat2Enabled();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isModCompatActive() {
        return modCompatActive;
    }

    public static void setModCompatActive(boolean active) {
        modCompatActive = active;
    }

    public static boolean isModCompat2Active() {
        return modCompatActive2;
    }

    public static void setModCompat2Active(boolean active) {
        modCompatActive2 = active;
    }
}
