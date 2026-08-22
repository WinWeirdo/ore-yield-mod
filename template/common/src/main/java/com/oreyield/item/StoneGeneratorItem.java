package com.oreyield.item;

import com.oreyield.advancement.OreYieldAdvancements;
//? if level_is_client_side_method {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Awards the Stone Generator advancement for crafting or receiving this item. */
public final class StoneGeneratorItem extends BlockItem {
    public StoneGeneratorItem(Block block, Properties properties) {
        super(block, properties);
    }

    //? if level_is_client_side_method {
    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        OreYieldAdvancements.onStoneGeneratorObtained(player);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof Player player) {
            OreYieldAdvancements.onStoneGeneratorObtained(player);
        }
    }
    //?} else {
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide) OreYieldAdvancements.onStoneGeneratorObtained(player);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player) {
            OreYieldAdvancements.onStoneGeneratorObtained(player);
        }
    }
    //?}
}
