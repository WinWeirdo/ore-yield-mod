package com.oreyield.forge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.loot.BreakRollStore;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BreakRollStoreEvents {
    private BreakRollStoreEvents() {}

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BreakRollStore.onLevelUnload(level);
        }
    }
}
