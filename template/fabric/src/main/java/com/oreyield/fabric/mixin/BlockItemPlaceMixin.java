package com.oreyield.fabric.mixin;

import com.oreyield.block.ProvenanceHostBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks successful BlockItem placements without maintaining any world-position cache. */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void oreYield$markPlayerPlacedHost(BlockPlaceContext context,
                                               CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction() || context.getPlayer() == null) return;
        if (context.getLevel() instanceof ServerLevel level) {
            ProvenanceHostBlocks.replaceWithPlayerPlaced(level, context.getClickedPos());
        }
    }
}
