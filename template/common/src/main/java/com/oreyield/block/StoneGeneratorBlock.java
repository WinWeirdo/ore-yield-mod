package com.oreyield.block;

import com.oreyield.config.OreConfig;
import net.minecraft.core.BlockPos;
//? if block_properties_require_id {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-ticked generator which only ever fills the air block directly above it. */
public final class StoneGeneratorBlock extends Block {
    //? if block_properties_require_id {
    public StoneGeneratorBlock(ResourceLocation id) {
        super(BlockBehaviour.Properties.of().strength(3.5F, 6.0F)
                .setId(ResourceKey.create(Registries.BLOCK, id)));
    }
    //?} else {
    public StoneGeneratorBlock() {
        super(BlockBehaviour.Properties.of().strength(3.5F, 6.0F));
    }
    //?}

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        //? if level_is_client_side_method {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
        //?} else {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
        //?}
            level.scheduleTick(pos, this, OreConfig.stoneGeneratorCooldownTicks());
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block targetBlock = hostBlockFor(level);
        BlockPos targetPos = pos.above();
        if (targetBlock != null && level.isEmptyBlock(targetPos)) {
            Block generatedHost = ProvenanceHostBlocks.generatedFor(targetBlock);
            level.setBlock(targetPos, (generatedHost != null ? generatedHost : targetBlock).defaultBlockState(), 3);
        }
        level.scheduleTick(pos, this, OreConfig.stoneGeneratorCooldownTicks());
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        // The normal loot table is deliberately empty: this explicit server-side
        // drop explicitly checks vanilla diamond/netherite pickaxes, avoiding
        // loader-specific harvest-tag timing and guaranteeing one self-drop.
        //? if level_is_client_side_method {
        if (!level.isClientSide() && !player.isCreative() && isHarvestTool(tool)) {
        //?} else {
        if (!level.isClientSide && !player.isCreative() && isHarvestTool(tool)) {
        //?}
            Block.popResource(level, pos, new ItemStack(this));
        }
    }

    private static boolean isHarvestTool(ItemStack tool) {
        return tool.is(Items.DIAMOND_PICKAXE) || tool.is(Items.NETHERITE_PICKAXE);
    }

    private static Block hostBlockFor(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) return Blocks.STONE;
        if (level.dimension() == Level.NETHER) return Blocks.NETHERRACK;
        if (level.dimension() == Level.END) return Blocks.END_STONE;
        // DimensionManager has no host-block mapping for custom dimensions.
        return null;
    }
}
