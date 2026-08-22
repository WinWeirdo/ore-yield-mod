package com.oreyield.fabric.mixin;

import com.oreyield.config.OreEntry;
import com.oreyield.advancement.OreYieldAdvancements;
import com.oreyield.loot.BreakRollStore;
import com.oreyield.loot.BreakContext;
import com.oreyield.loot.MineralPocketResult;
import com.oreyield.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
//? if enchant_holder_api {
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Fabric's equivalent of the Forge/NeoForge global loot modifier. Running here gives
 * player, automated, and explosion drops one completed loot-path implementation instead
 * of applying player rolls later in a separate block-break callback.
 */
@Mixin(Block.class)
public abstract class BlockDropMixin {
    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL"))
    private static void oreYield$dropOreExtras(BlockState state, Level level, BlockPos pos,
                                               BlockEntity blockEntity, Entity entity, ItemStack tool,
                                               CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Player player = entity instanceof Player p ? p : null;
        if (player != null && player.isCreative()) return;
        if (tool == null) tool = ItemStack.EMPTY;
        // Loot modifiers receive a dedicated LootContext random on Forge and
        // NeoForge.  Use an independent source here rather than Level's shared
        // gameplay RNG so Fabric rolls cannot be affected by other world logic.
        RandomSource random = RandomSource.create();
        BreakContext breakContext = player != null
                ? BreakContext.player(Services.PLATFORM.isFakePlayer(player))
                : BreakContext.AUTOMATED;
        //? if enchant_holder_api {
        Holder<net.minecraft.world.item.enchantment.Enchantment> blockFortune =
                serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE);
        //?} else {
        net.minecraft.world.item.enchantment.Enchantment blockFortune = Enchantments.BLOCK_FORTUNE;
        //?}
        //? if enchant_helper_stack_first {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(tool, blockFortune);
        //?} else {
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(blockFortune, tool);
        //?}

        List<OreEntry> hits = BreakRollStore.takeOrRoll(serverLevel, pos, state, tool,
                random, player, breakContext);
        int totalXp = 0;
        for (OreEntry hit : hits) {
            if (!hit.meetsPickaxeRequirement(tool, player)) continue;
            ItemStack extra = hit.createDrop(random, fortune);
            if (!extra.isEmpty()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra);
                OreYieldAdvancements.onOreDrop(player, state, extra);
                totalXp += hit.rollXp(random);
            }
        }
        MineralPocketResult pocket = BreakRollStore.takeOrRollMineralPocket(serverLevel, pos, state, tool,
                random, player, breakContext);
        for (ItemStack extra : pocket.drops()) {
            Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra);
        }
        pocket.announce(player);
        OreYieldAdvancements.onMineralPocket(player, pocket);
        if (totalXp > 0) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), totalXp);
        }
    }
}
