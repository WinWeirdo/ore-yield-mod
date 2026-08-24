package com.oreyield.loot;

import com.oreyield.config.MineralPocketType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Extra drops created by one successful mineral-pocket roll. */
public record MineralPocketResult(MineralPocketType type, List<ItemStack> drops) {
    private static final MineralPocketResult NONE = new MineralPocketResult(null, List.of());

    public MineralPocketResult {
        drops = List.copyOf(drops);
    }

    public static MineralPocketResult none() {
        return NONE;
    }

    public boolean found() {
        return type != null && !drops.isEmpty();
    }

    public int experience() {
        return found() ? type.experience() : 0;
    }

    public void announce(Player player) {
        if (player == null || !found()) return;
        Component message = Component.translatable("message.ore_yield.mineral_pocket",
                Component.translatable("message.ore_yield.mineral_pocket." + type.configKey()), experience());
        //? if player_display_client_message {
        player.displayClientMessage(message, true);
        //?} else {
        player.sendSystemMessage(message);
        //?}
    }
}
