package com.oreyield.fabric.mixin;

import com.oreyield.config.OreEntry;
import com.oreyield.loot.BreakRollStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Forge parity for non-player block destruction: explosions, pistons, falling blocks,
 * etc. go through the loot system on Forge (global loot modifier) but have no hook on
 * Fabric, so ore rolls are re-run here. Player breaks are skipped — they are handled by
 * BreakHandlerFabric. The empty tool with a null player mirrors the Forge behaviour of
 * "explosions yield all ores".
 */
@Mixin(Block.class)
public abstract class BlockDropMixin {
    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL"))
    private static void oreYield$dropOreExtras(BlockState state, Level level, BlockPos pos,
                                               BlockEntity blockEntity, Entity entity, ItemStack tool,
                                               CallbackInfo ci) {
        if (entity instanceof Player) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<OreEntry> hits = BreakRollStore.takeOrRoll(serverLevel, pos, state, ItemStack.EMPTY,
                serverLevel.random, null);
        for (OreEntry hit : hits) {
            if (!hit.meetsPickaxeRequirement(ItemStack.EMPTY, null)) continue;
            ItemStack extra = hit.createDrop(serverLevel.random, 0);
            if (!extra.isEmpty()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra);
            }
        }
    }
}
