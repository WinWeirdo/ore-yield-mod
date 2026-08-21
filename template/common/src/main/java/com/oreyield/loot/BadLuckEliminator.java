package com.oreyield.loot;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player "bad luck eliminator": each ore tracks consecutive eligible block breaks without a drop.
 * Once the failure counter reaches the guarantee window (default: ceil(2.0 / chance) eligible blocks),
 * the next eligible block is guaranteed to drop that ore, ending any bad-luck streak.
 */
public final class BadLuckEliminator {
    private static final Map<UUID, Map<String, Integer>> FAILURES = new ConcurrentHashMap<>();

    private BadLuckEliminator() {}

    public static boolean isEnabled() {
        return OreConfig.isBadLuckEliminatorEnabled();
    }

    /** Guarantee window in eligible blocks; Integer.MAX_VALUE when pity does not apply (chance 0 or >= 1). */
    public static int window(OreEntry entry) {
        return window(entry, 1.0D);
    }

    public static int window(OreEntry entry, double chanceMultiplier) {
        double chance = Math.min(1.0D, entry.chance() * Math.max(0.0D, chanceMultiplier));
        if (chance <= 0.0D || chance >= 1.0D) return Integer.MAX_VALUE;
        return (int) Math.ceil(OreConfig.badLuckMultiplier() / chance);
    }

    /** A block break counts as a failure for this ore only if it could have dropped it. */
    public static boolean isEligible(OreEntry entry, BlockState state, String dimension, BlockPos pos, ItemStack tool, Player player) {
        return isEligible(entry, state, dimension, pos, tool, player, 1.0D);
    }

    public static boolean isEligible(OreEntry entry, BlockState state, String dimension, BlockPos pos, ItemStack tool, Player player,
                                     double chanceMultiplier) {
        if (!isEnabled()) return false;
        if (!entry.enabled()) return false;
        double chance = Math.min(1.0D, entry.chance() * Math.max(0.0D, chanceMultiplier));
        if (chance <= 0.0D || chance >= 1.0D) return false;
        if (!entry.matches(state, dimension)) return false;
        if (!entry.canRollAt(pos, dimension)) return false;
        return entry.meetsPickaxeRequirement(tool, player);
    }

    /** True when this break must yield the ore at 100% to end the streak. */
    public static boolean shouldForceDrop(Player player, String counterId, OreEntry entry, double chanceMultiplier) {
        if (!isEnabled()) return false;
        Map<String, Integer> perOre = FAILURES.get(player.getUUID());
        if (perOre == null) return false;
        Integer fails = perOre.get(counterId);
        return fails != null && fails >= window(entry, chanceMultiplier) - 1;
    }

    /** Records the outcome of one eligible break. A hit resets the counter; a miss increments it. */
    public static void advance(Player player, String oreId, boolean hit) {
        UUID uuid = player.getUUID();
        if (hit) {
            Map<String, Integer> perOre = FAILURES.get(uuid);
            if (perOre != null) {
                perOre.remove(oreId);
                if (perOre.isEmpty()) FAILURES.remove(uuid);
            }
            return;
        }
        FAILURES.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).merge(oreId, 1, Integer::sum);
    }

    public static void clearCounters() {
        FAILURES.clear();
    }

    public static int failures(UUID uuid, String oreId) {
        Map<String, Integer> perOre = FAILURES.get(uuid);
        return perOre == null ? 0 : perOre.getOrDefault(oreId, 0);
    }
}
