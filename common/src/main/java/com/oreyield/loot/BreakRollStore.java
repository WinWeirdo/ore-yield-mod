package com.oreyield.loot;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shares one set of independent ore rolls between the break event (XP) and the drop handler (items). */
public final class BreakRollStore {
    private static final Map<ServerLevel, Map<BlockPos, List<OreEntry>>> PENDING = new HashMap<>();
    private static final int MAX_PENDING_PER_LEVEL = 1024;

    private BreakRollStore() {}

    /** Loader modules call this from their level-unload listener. */
    public static void onLevelUnload(ServerLevel level) {
        PENDING.remove(level);
    }

    public static List<OreEntry> prepare(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, Player player) {
        if (hasSilkTouch(tool)) return List.of();
        Map<BlockPos, List<OreEntry>> byPosition = PENDING.computeIfAbsent(level, k -> new HashMap<>());
        BlockPos immutable = pos.immutable();
        List<OreEntry> existing = byPosition.get(immutable);
        if (existing != null) return existing;
        List<OreEntry> rolls = roll(level, pos, state, level.random, tool, player);
        if (byPosition.size() >= MAX_PENDING_PER_LEVEL) {
            // Cap reached — skip caching this block (rolls will be recomputed in takeOrRoll fallback)
            return rolls;
        }
        byPosition.put(immutable, rolls);
        return rolls;
    }

    public static List<OreEntry> takeOrRoll(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool,
                                            RandomSource random, Player player) {
        Map<BlockPos, List<OreEntry>> byPosition = PENDING.get(level);
        if (byPosition != null) {
            List<OreEntry> saved = byPosition.remove(pos);
            if (saved != null) {
                if (byPosition.isEmpty()) PENDING.remove(level);
                return saved;
            }
        }
        return hasSilkTouch(tool) ? List.of() : roll(level, pos, state, random, tool, player);
    }

    private static List<OreEntry> roll(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                                       ItemStack tool, Player player) {
        String dimension = getDimension(level);
        List<OreEntry> hits = new ArrayList<>();
        for (OreEntry entry : OreConfig.entriesFor(state, dimension)) {
            boolean hit = entry.rollsAt(pos, random, dimension);
            if (!hit && player != null && BadLuckEliminator.shouldForceDrop(player, entry)) {
                hit = true;
            }
            if (hit) hits.add(entry);
            if (player != null && BadLuckEliminator.isEligible(entry, state, dimension, pos, tool, player)) {
                BadLuckEliminator.advance(player, entry.id(), hit);
            }
        }
        return List.copyOf(hits);
    }

    private static String getDimension(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        ResourceLocation loc = key.location();
        if (loc.equals(Level.OVERWORLD.location())) return "minecraft:overworld";
        if (loc.equals(Level.NETHER.location())) return "minecraft:the_nether";
        if (loc.equals(Level.END.location())) return "minecraft:the_end";
        return loc.toString();
    }

    private static boolean hasSilkTouch(ItemStack tool) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, tool) > 0;
    }
}
