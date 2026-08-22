package com.oreyield.loot;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import com.oreyield.advancement.OreYieldAdvancements;
import com.oreyield.block.HostOrigin;
import com.oreyield.block.ProvenanceHostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
                                             RandomSource random, Player player, BreakContext context) {
        if (!allowsOreYield(state, context)) return List.of();
        return hasSilkTouch(level, tool) ? List.of() : roll(level, pos, state, random, tool, player, context);
    }

    /**
     * Rolls an optional mineral pocket after normal block loot has been accepted.
     * This path deliberately never advances Bad Luck Eliminator counters or applies Fortune.
     */
    public static MineralPocketResult takeOrRollMineralPocket(ServerLevel level, BlockPos pos, BlockState state,
                                                               ItemStack tool, RandomSource random, Player player,
                                                               BreakContext context) {
        if (!OreConfig.isMineralPocketsEnabled() || context.explosion()) return MineralPocketResult.none();
        if (context.automated() && !OreConfig.allowsMineralPocketAutomatedHarvesting()) return MineralPocketResult.none();
        if (hasSilkTouch(level, tool)) return MineralPocketResult.none();

        String dimension = getDimension(level);
        if (!OreConfig.isMineralPocketDimensionEnabled(dimension)) return MineralPocketResult.none();

        HostOrigin origin = ProvenanceHostBlocks.originOf(state);
        if (origin == HostOrigin.GENERATED && !OreConfig.allowsMineralPocketsOnGenerator()) return MineralPocketResult.none();
        if (origin == HostOrigin.PLAYER_PLACED && !OreConfig.allowsPlayerPlacedEligibleBlocks()) return MineralPocketResult.none();

        BlockState canonical = ProvenanceHostBlocks.canonicalState(state);
        if (!isMineralPocketHost(state, canonical, dimension) || !tool.isCorrectToolForDrops(canonical)) {
            return MineralPocketResult.none();
        }
        return MineralPocketRoller.roll(random);
    }

    private static List<OreEntry> roll(ServerLevel level, BlockPos pos, BlockState state, RandomSource random,
                                       ItemStack tool, Player player, BreakContext context) {
        String dimension = getDimension(level);
        HostOrigin origin = ProvenanceHostBlocks.originOf(state);
        boolean generatorOutput = origin == HostOrigin.GENERATED;
        double chanceMultiplier = generatorOutput ? OreConfig.generatorOreYieldChanceMultiplier() : 1.0D;
        List<OreEntry> entries = OreConfig.entriesFor(state, dimension);
        LOGGER.debug("[Ore Yield] roll block={} dim={} pos={} entries={} tool={}",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), dimension, pos,
                entries.size(), tool);
        List<OreEntry> hits = new ArrayList<>();
        for (OreEntry entry : entries) {
            // A malformed or unavailable result item must not consume chance or reset pity.
            if (!entry.hasAvailableResultItem()) continue;
            boolean hit = entry.rollsAt(pos, random, dimension, chanceMultiplier);
            boolean eligible = player != null && !context.automated()
                    && BadLuckEliminator.isEligible(entry, state, dimension, pos, tool, player, chanceMultiplier);
            String counterId = generatorOutput ? entry.id() + "|generator" : entry.id();
            if (!hit && eligible && BadLuckEliminator.shouldForceDrop(player, counterId, entry, chanceMultiplier)) {
                hit = true;
                OreYieldAdvancements.onBadLuckEliminatorActivated(player);
            }
            if (hit) hits.add(entry);
            if (eligible) {
                BadLuckEliminator.advance(player, counterId, hit);
            }
        }
        if (!hits.isEmpty()) {
            LOGGER.debug("[Ore Yield] roll hits={}", hits.stream().map(OreEntry::id).toList());
        }
        return List.copyOf(hits);
    }

    private static boolean allowsOreYield(BlockState state, BreakContext context) {
        HostOrigin origin = ProvenanceHostBlocks.originOf(state);
        if (origin == HostOrigin.PLAYER_PLACED) {
            return OreConfig.allowsPlayerPlacedEligibleBlocks();
        }
        if (origin != HostOrigin.GENERATED || !OreConfig.isGeneratorOreYieldEnabled()) return origin != HostOrigin.GENERATED;
        if (context.explosion()) return OreConfig.allowsGeneratorExplosionHarvesting();
        return !context.automated() || OreConfig.allowsGeneratorAutomatedHarvesting();
    }

    private static boolean isMineralPocketHost(BlockState state, BlockState canonical, String dimension) {
        if ("minecraft:the_end".equals(dimension)) return canonical.is(Blocks.END_STONE);
        return "minecraft:overworld".equals(dimension) && !OreConfig.entriesFor(state, dimension).isEmpty();
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
