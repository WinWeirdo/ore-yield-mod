package com.oreyield.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry-independent definitions and lookups for host blocks with an origin. */
public final class ProvenanceHostBlocks {
    private static final Map<Block, Block> GENERATED = new LinkedHashMap<>();
    private static final Map<Block, Block> PLAYER_PLACED = new LinkedHashMap<>();

    private ProvenanceHostBlocks() {}

    public static List<Definition> definitions() {
        return List.of(
                new Definition("generated_stone", Blocks.STONE, HostOrigin.GENERATED),
                new Definition("generated_netherrack", Blocks.NETHERRACK, HostOrigin.GENERATED),
                new Definition("generated_end_stone", Blocks.END_STONE, HostOrigin.GENERATED),
                new Definition("player_placed_stone", Blocks.STONE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_deepslate", Blocks.DEEPSLATE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_tuff", Blocks.TUFF, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_andesite", Blocks.ANDESITE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_granite", Blocks.GRANITE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_diorite", Blocks.DIORITE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_calcite", Blocks.CALCITE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_netherrack", Blocks.NETHERRACK, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_blackstone", Blocks.BLACKSTONE, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_basalt", Blocks.BASALT, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_smooth_basalt", Blocks.SMOOTH_BASALT, HostOrigin.PLAYER_PLACED),
                new Definition("player_placed_end_stone", Blocks.END_STONE, HostOrigin.PLAYER_PLACED)
        );
    }

    static void register(Block source, Block marker, HostOrigin origin) {
        (origin == HostOrigin.GENERATED ? GENERATED : PLAYER_PLACED).put(source, marker);
    }

    public static Block generatedFor(Block source) {
        return GENERATED.get(source);
    }

    public static boolean replaceWithPlayerPlaced(ServerLevel level, BlockPos pos) {
        Block marker = PLAYER_PLACED.get(level.getBlockState(pos).getBlock());
        return marker != null && level.setBlock(pos, marker.defaultBlockState(), 3);
    }

    public static HostOrigin originOf(BlockState state) {
        return state.getBlock() instanceof ProvenanceHostBlock marker ? marker.origin() : null;
    }

    /** Returns the vanilla backing state used for tags, config ids, and host matching. */
    public static BlockState canonicalState(BlockState state) {
        return state.getBlock() instanceof ProvenanceHostBlock marker
                ? marker.source().defaultBlockState()
                : state;
    }

    public record Definition(String id, Block source, HostOrigin origin) {}
}
