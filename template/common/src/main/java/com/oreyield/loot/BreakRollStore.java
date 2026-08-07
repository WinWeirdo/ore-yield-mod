package com.oreyield.loot;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Rolls only after a successful drop path, avoiding stale pre-break state. */
public final class BreakRollStore {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ore_yield/Roll");

    private BreakRollStore() {}

    /** Retained for loader compatibility; rolls are no longer cached per level. */
    public static void onLevelUnload(ServerLevel level) {
    }

    /**
     * Legacy pre-break hook. Do not roll here: a cancelled break must not alter
     * pity counters or award XP. Loaders should use {@link #takeOrRoll} after
     * the loot path has been reached.
     */
    public static List<OreEntry> prepare(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, Player player) {
        return List.of();
    }

    public static List<OreEntry> takeOrRoll(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool,
                                             RandomSource random, Player player) {
        return hasSilkTouch(level, tool) ? List.of() : roll(level, pos, state, random, tool, player);
    }

    private static List<OreEntry> roll(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                                       ItemStack tool, Player player) {
        String dimension = getDimension(level);
        List<OreEntry> entries = OreConfig.entriesFor(state, dimension);
        LOGGER.debug("[Ore Yield] roll block={} dim={} pos={} entries={} tool={}",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), dimension, pos,
                entries.size(), tool);
        List<OreEntry> hits = new ArrayList<>();
        for (OreEntry entry : entries) {
            boolean hit = entry.rollsAt(pos, random, dimension);
            boolean eligible = player != null
                    && BadLuckEliminator.isEligible(entry, state, dimension, pos, tool, player);
            if (!hit && eligible && BadLuckEliminator.shouldForceDrop(player, entry)) {
                hit = true;
            }
            if (hit) hits.add(entry);
            if (eligible) {
                BadLuckEliminator.advance(player, entry.id(), hit);
            }
        }
        if (!hits.isEmpty()) {
            LOGGER.debug("[Ore Yield] roll hits={}", hits.stream().map(OreEntry::id).toList());
        }
        return List.copyOf(hits);
    }

    private static String getDimension(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        //? if resourcekey_identifier {
        Identifier loc = key.identifier();
        if (loc.equals(Level.OVERWORLD.identifier())) return "minecraft:overworld";
        if (loc.equals(Level.NETHER.identifier())) return "minecraft:the_nether";
        if (loc.equals(Level.END.identifier())) return "minecraft:the_end";
        //?} else {
        ResourceLocation loc = key.location();
        if (loc.equals(Level.OVERWORLD.location())) return "minecraft:overworld";
        if (loc.equals(Level.NETHER.location())) return "minecraft:the_nether";
        if (loc.equals(Level.END.location())) return "minecraft:the_end";
        //?}
        return loc.toString();
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        //? if enchant_holder_api {
        Holder<net.minecraft.world.item.enchantment.Enchantment> silkTouch =
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
        //?} else {
        net.minecraft.world.item.enchantment.Enchantment silkTouch = net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
        //?}
        //? if item_instance_api {
        // At 26.x ItemStack implements ItemInstance, so the tool can be passed as-is (holder-first signature).
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
        //?} else if enchant_helper_stack_first {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(tool, silkTouch) > 0;
        //?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(silkTouch, tool) > 0;
        //?}
    }
}
