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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ore_yield/Break");
    private BreakHandlerFabric() {}

    /**
     * Runs only after Fabric has completed the block break.  Using BEFORE here used
     * to award items and XP even when another callback later cancelled the break.
     */
    public static void onAfterBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        //? if level_is_client_side_method {
        if (world.isClientSide() || !(world instanceof ServerLevel level)) return;
        //?} else {
        if (world.isClientSide || !(world instanceof ServerLevel level)) return;
        //?}
        if (player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        List<OreEntry> rolls = BreakRollStore.takeOrRoll(level, pos, state, tool, level.getRandom(), player);
        LOGGER.debug("[Ore Yield] processed successful player break: block={} dim={} pos={} creative={} tool={} rolls={}",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), level.dimension(), pos,
                player.isCreative(), tool, rolls.size());
        //? if enchant_holder_api {
        net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> blockFortune =
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        //?} else {
        net.minecraft.world.item.enchantment.Enchantment blockFortune = net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE;
        //?}
        //? if enchant_helper_stack_first {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(tool, blockFortune);
        //?} else {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(blockFortune, tool);
        //?}
        int totalXp = 0;
        for (OreEntry hit : rolls) {
            if (!hit.meetsPickaxeRequirement(tool, player)) continue;
            ItemStack extra = hit.createDrop(level.getRandom(), fortune);
            if (!extra.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra);
                LOGGER.debug("[Ore Yield] dropped {} x{}", extra.getItem(), extra.getCount());
            }
            totalXp += hit.rollXp(level.getRandom());
        }
        if (totalXp > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), totalXp);
        }
    }
}
