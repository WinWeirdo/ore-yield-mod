package com.oreyield.fabric;

import com.oreyield.OreYieldMod;
import com.oreyield.fabric.event.BreakHandlerFabric;
import com.oreyield.fabric.worldgen.OreRemovalFabric;
import com.oreyield.loot.BreakRollStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//? if fabric_server_level_events {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
//?} else {
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
//?}
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class OreYieldModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OreYieldMod.init();
        // AFTER is only fired for a completed break; rewards must never be paid for a
        // break that another mod or a protection plugin cancels in BEFORE.
        PlayerBlockBreakEvents.AFTER.register(BreakHandlerFabric::onAfterBreak);
        ServerLifecycleEvents.SERVER_STARTING.register(OreYieldMod::discoverModdedDimensions);
        //? if fabric_server_level_events {
        ServerLevelEvents.UNLOAD.register((server, world) -> BreakRollStore.onLevelUnload(world));
        //?} else {
        ServerWorldEvents.UNLOAD.register((server, world) -> BreakRollStore.onLevelUnload(world));
        //?}
        OreRemovalFabric.register();
    }
}
