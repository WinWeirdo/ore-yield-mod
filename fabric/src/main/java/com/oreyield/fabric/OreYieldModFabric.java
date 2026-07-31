package com.oreyield.fabric;

import com.oreyield.OreYieldMod;
import com.oreyield.fabric.event.BreakHandlerFabric;
import com.oreyield.fabric.worldgen.OreRemovalFabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class OreYieldModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OreYieldMod.init();
        PlayerBlockBreakEvents.BEFORE.register(BreakHandlerFabric::onBreak);
        ServerLifecycleEvents.SERVER_STARTING.register(OreYieldMod::discoverModdedDimensions);
        OreRemovalFabric.register();
    }
}
