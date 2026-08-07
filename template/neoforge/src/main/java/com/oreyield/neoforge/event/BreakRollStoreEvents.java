package com.oreyield.neoforge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.loot.BreakRollStore;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

//? if eventbus_bus_attribute {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
//?} else {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID)
//?}
public final class BreakRollStoreEvents {
    private BreakRollStoreEvents() {}

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BreakRollStore.onLevelUnload(level);
        }
    }
}
