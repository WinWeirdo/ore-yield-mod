package com.oreyield.config;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record OreEntry(String id, boolean enabled, List<String> hosts, String resultItem, int minCount, int maxCount,
                       double chance, int minY, int maxY, int peakY, FortuneType fortuneType, int xpMin, int xpMax,
                       String dimension, int minPickaxeLevel) {

    /**
     * Maps a tool ItemStack to a numeric pickaxe level.
     * Returns 3 (diamond) if tool is empty (explosions yield all ores).
     *   0 = wooden, 1 = stone, 2 = iron, 3 = diamond/netherite
     */
    public static int getPickaxeLevel(ItemStack tool) {
        if (tool.isEmpty()) return 3;
        if (!(tool.getItem() instanceof PickaxeItem pickaxe)) return -1;
        int level = pickaxe.getTier().getLevel();
        return Math.max(0, Math.min(level, 3));
    }

    /**
     * Pickaxe requirement check. An empty hand is never a pickaxe: only non-player
     * breaks (explosions, pistons) may bypass the tool requirement, so their drops
     * work like the old "empty tool = diamond level" behaviour.
     */
    public boolean meetsPickaxeRequirement(ItemStack tool, Player player) {
        if (minPickaxeLevel <= 0) return true;
        if (tool.isEmpty()) return player == null;
        int level = getPickaxeLevel(tool);
        return level >= minPickaxeLevel;
    }

    private static final Map<String, TagKey<net.minecraft.world.level.block.Block>> TAG_CACHE = new ConcurrentHashMap<>();

    private static TagKey<net.minecraft.world.level.block.Block> getCachedTag(String host) {
        return TAG_CACHE.computeIfAbsent(host, h -> {
            ResourceLocation tagId = ResourceLocation.tryParse(h.substring(1));
            return tagId != null ? TagKey.create(Registries.BLOCK, tagId) : null;
        });
    }

    public boolean matches(BlockState state, String currentDimension) {
        boolean inEnd = currentDimension.equals("minecraft:the_end");
        boolean endOverride = inEnd
                && com.oreyield.config.OreConfig.isModCompat2Enabled()
                && com.oreyield.config.OreConfig.isModCompat2OresInEnd()
                && com.oreyield.compat.ModCompat2Manager.isCuratedEntry(id)
                && !com.oreyield.compat.ModCompat2Manager.hasEndVariant(id);
        boolean dimensionMatch = dimension.isEmpty() || dimension.equals(currentDimension) || endOverride;
        if (!dimensionMatch) return false;
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (endOverride && "minecraft:end_stone".equals(blockId.toString())) return true;
        for (String host : hosts) {
            if (host.startsWith("#")) {
                TagKey<net.minecraft.world.level.block.Block> tag = getCachedTag(host);
                if (tag != null && state.is(tag)) return true;
            } else if (host.equals(blockId.toString())) {
                return true;
            }
        }
        if (com.oreyield.OreYieldMod.isModCompatActive()
                && com.oreyield.compat.ModCompatManager.isAutoDetectedHost(blockId.toString())) {
            return true;
        }
        if (com.oreyield.OreYieldMod.isModCompat2Active()
                && com.oreyield.compat.ModCompat2Manager.isAutoDetectedHost(blockId.toString())) {
            return true;
        }
        return false;
    }

    /** Deterministic part of the roll: enabled flag + dimension/Y restrictions (no randomness). */
    public boolean canRollAt(BlockPos pos, String dimension) {
        if (!enabled) return false;
        boolean inEnd = "minecraft:the_end".equals(dimension);
        boolean skipY = inEnd && com.oreyield.config.OreConfig.isModCompat2OresInEnd();
        if (!skipY && (pos.getY() < minY || pos.getY() > maxY)) return false;
        return true;
    }

    public boolean rollsAt(BlockPos pos, RandomSource random, String dimension) {
        if (!canRollAt(pos, dimension)) return false;
        double adjustedChance = chance;
        if (peakY >= 0 && peakY >= minY && peakY <= maxY && minY != maxY) {
            boolean inEnd = "minecraft:the_end".equals(dimension);
            boolean skipY = inEnd && com.oreyield.config.OreConfig.isModCompat2OresInEnd();
            if (!skipY) {
                adjustedChance *= pos.getY() <= peakY
                        ? (double) (pos.getY() - minY + 1) / (peakY - minY + 1)
                        : (double) (maxY - pos.getY() + 1) / (maxY - peakY + 1);
            }
        }
        return random.nextDouble() < adjustedChance;
    }

    public ItemStack createDrop(RandomSource random, int fortuneLevel) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(resultItem));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        int count;
        if (fortuneType == FortuneType.REDSTONE) {
            // Discrete uniform: each drop amount between minCount and (maxCount + fortuneLevel) equally likely
            count = minCount + random.nextInt(maxCount - minCount + 1 + fortuneLevel);
        } else {
            count = minCount + random.nextInt(maxCount - minCount + 1);
            if (fortuneType == FortuneType.ORE && fortuneLevel > 0) {
                // 2/(level+2) chance of no bonus (1x); otherwise uniform chance for multiplier 2 to level+1
                if (random.nextDouble() >= 2.0 / (fortuneLevel + 2)) {
                    count *= 2 + random.nextInt(fortuneLevel);
                }
            }
        }
        count = Math.max(1, count);
        return new ItemStack(item, count);
    }

    public int rollXp(RandomSource random) {
        return xpMax <= xpMin ? xpMin : xpMin + random.nextInt(xpMax - xpMin + 1);
    }
}
