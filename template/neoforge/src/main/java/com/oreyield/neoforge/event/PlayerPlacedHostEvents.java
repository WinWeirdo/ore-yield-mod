package com.oreyield.neoforge.event;

import com.oreyield.block.ProvenanceHostBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Replaces standard player-placed host blocks with persistent origin markers. */
public final class PlayerPlacedHostEvents {
    private PlayerPlacedHostEvents() {}

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getLevel() instanceof ServerLevel level) {
            ProvenanceHostBlocks.replaceWithPlayerPlaced(level, event.getPos());
        }
    }
}
