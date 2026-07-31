package com.oreyield.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oreyield.config.OreEntry;
import com.oreyield.loot.BreakRollStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;

import java.util.List;

public final class OreYieldLootModifier extends LootModifier {
    public static final Codec<OreYieldLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, OreYieldLootModifier::new));

    public OreYieldLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.hasParam(LootContextParams.BLOCK_STATE) || !context.hasParam(LootContextParams.ORIGIN)) return generatedLoot;
        BlockState state = context.getParam(LootContextParams.BLOCK_STATE);
        BlockPos pos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        Player player = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player p ? p : null;
        List<OreEntry> hits = BreakRollStore.takeOrRoll(context.getLevel(), pos, state, tool, context.getRandom(), player);
        for (OreEntry hit : hits) {
            if (!hit.meetsPickaxeRequirement(tool)) continue;
            ItemStack extra = hit.createDrop(context.getRandom(), fortune);
            if (!extra.isEmpty()) generatedLoot.add(extra);
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
