package com.oreyield.fabric.event;

import com.oreyield.config.OreEntry;
import com.oreyield.loot.BreakRollStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BreakHandlerFabric {
    private BreakHandlerFabric() {}

    public static boolean onBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClientSide || !(world instanceof ServerLevel level)) return true;
        if (player.isCreative()) return true;

        ItemStack tool = player.getMainHandItem();
        List<OreEntry> rolls = BreakRollStore.takeOrRoll(level, pos, state, tool, level.random, player);
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        int totalXp = 0;
        for (OreEntry hit : rolls) {
            if (!hit.meetsPickaxeRequirement(tool, player)) continue;
            ItemStack extra = hit.createDrop(level.random, fortune);
            if (!extra.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra);
            }
            totalXp += hit.rollXp(level.random);
        }
        if (totalXp > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), totalXp);
        }
        return true;
    }
}
