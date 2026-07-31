package com.oreyield.forge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.config.OreEntry;
import com.oreyield.loot.BreakRollStore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BreakXpHandler {
    private BreakXpHandler() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer() != null && event.getPlayer().isCreative()) return;
        ItemStack tool = event.getPlayer() == null ? ItemStack.EMPTY : event.getPlayer().getMainHandItem();
        int totalXp = 0;
        for (OreEntry hit : BreakRollStore.prepare(level, event.getPos(), event.getState(), tool, event.getPlayer())) {
            if (!hit.meetsPickaxeRequirement(tool)) continue;
            totalXp += hit.rollXp(level.random);
        }
        if (totalXp > 0) {
            ExperienceOrb.award(level, net.minecraft.world.phys.Vec3.atCenterOf(event.getPos()), totalXp);
        }
    }
}
