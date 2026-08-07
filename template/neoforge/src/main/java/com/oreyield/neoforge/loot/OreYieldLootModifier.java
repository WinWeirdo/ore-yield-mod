package com.oreyield.neoforge.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oreyield.config.OreEntry;
import com.oreyield.loot.BreakRollStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

public final class OreYieldLootModifier extends LootModifier {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ore_yield/LootMod");
    public static final MapCodec<OreYieldLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, OreYieldLootModifier::new));

    //? if loot_modifier_priority {
    public OreYieldLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }
    //?} else {
    public OreYieldLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }
    //?}

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        //? if loot_context_parameter_api {
        if (!context.hasParameter(LootContextParams.BLOCK_STATE) || !context.hasParameter(LootContextParams.ORIGIN)) {
            LOGGER.debug("[Ore Yield] Skipping loot context without block state/origin parameters");
            return generatedLoot;
        }
        BlockState state = context.getParameter(LootContextParams.BLOCK_STATE);
        BlockPos pos = BlockPos.containing(context.getParameter(LootContextParams.ORIGIN));
        //?} else {
        if (!context.hasParam(LootContextParams.BLOCK_STATE) || !context.hasParam(LootContextParams.ORIGIN)) {
            LOGGER.debug("[Ore Yield] Skipping loot context without block state/origin parameters");
            return generatedLoot;
        }
        BlockState state = context.getParam(LootContextParams.BLOCK_STATE);
        BlockPos pos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
        //?}
        //? if item_instance_api {
        net.minecraft.world.item.ItemInstance item = context.getOptionalParameter(LootContextParams.TOOL);
        ItemStack tool = item instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        //?} else if loot_context_parameter_api {
        ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        //?} else {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) tool = ItemStack.EMPTY;
        //?}
        //? if enchant_holder_api {
        Holder<Enchantment> blockFortune = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        //?} else {
        Enchantment blockFortune = Enchantments.BLOCK_FORTUNE;
        //?}
        //? if enchant_helper_stack_first {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(tool, blockFortune);
        //?} else {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(blockFortune, tool);
        //?}
        //? if loot_context_parameter_api {
        Player player = context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player p ? p : null;
        //?} else {
        Player player = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player p ? p : null;
        //?}
        if (player != null && player.isCreative()) return generatedLoot;
        List<OreEntry> hits = BreakRollStore.takeOrRoll(context.getLevel(), pos, state, tool, context.getRandom(), player);
        LOGGER.debug("[Ore Yield] Applying ore rolls: state={} pos={} tool={} fortune={} hits={}",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), pos, tool, fortune,
                hits.size());
        int totalXp = 0;
        for (OreEntry hit : hits) {
            if (!hit.meetsPickaxeRequirement(tool, player)) continue;
            ItemStack extra = hit.createDrop(context.getRandom(), fortune);
            if (!extra.isEmpty()) generatedLoot.add(extra);
            totalXp += hit.rollXp(context.getRandom());
        }
        // A global loot modifier runs only while successful block loot is being generated.
        // Keeping XP here prevents canceled break attempts from advancing pity or awarding XP.
        if (totalXp > 0 && (player == null || !player.isCreative())) {
            ExperienceOrb.award(context.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(pos), totalXp);
        }
        LOGGER.debug("[Ore Yield] Ore rolls complete: generatedLoot={} state={}",
                generatedLoot.size(), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
