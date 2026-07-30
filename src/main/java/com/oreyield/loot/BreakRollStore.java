package com.oreyield.loot;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shares one set of independent ore rolls between BreakEvent (XP) and the global loot modifier (items). */
@Mod.EventBusSubscriber(modid = com.oreyield.OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BreakRollStore {
    private static final Map<ServerLevel, Map<BlockPos, List<OreEntry>>> PENDING = new HashMap<>();
    private static final int MAX_PENDING_PER_LEVEL = 1024;

    private BreakRollStore() {}

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING.remove(level);
        }
    }

    public static List<OreEntry> prepare(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (hasSilkTouch(tool)) return List.of();
        Map<BlockPos, List<OreEntry>> byPosition = PENDING.computeIfAbsent(level, k -> new HashMap<>());
        BlockPos immutable = pos.immutable();
        List<OreEntry> existing = byPosition.get(immutable);
        if (existing != null) return existing;
        if (byPosition.size() >= MAX_PENDING_PER_LEVEL) {
            // Cap reached — skip caching this block (rolls will be recomputed in takeOrRoll fallback)
            return roll(state, pos, level.random, getDimension(level));
        }
        String dimension = getDimension(level);
        List<OreEntry> rolls = roll(state, pos, level.random, dimension);
        byPosition.put(immutable, rolls);
        return rolls;
    }

    public static List<OreEntry> takeOrRoll(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, RandomSource random) {
        Map<BlockPos, List<OreEntry>> byPosition = PENDING.get(level);
        if (byPosition != null) {
            List<OreEntry> saved = byPosition.remove(pos);
            if (saved != null) {
                if (byPosition.isEmpty()) PENDING.remove(level);
                return saved;
            }
        }
        String dimension = getDimension(level);
        return hasSilkTouch(tool) ? List.of() : roll(state, pos, random, dimension);
    }

    private static List<OreEntry> roll(BlockState state, BlockPos pos, RandomSource random, String dimension) {
        List<OreEntry> hits = new ArrayList<>();
        for (OreEntry entry : OreConfig.entriesFor(state, dimension)) if (entry.rollsAt(pos, random, dimension)) hits.add(entry);
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
