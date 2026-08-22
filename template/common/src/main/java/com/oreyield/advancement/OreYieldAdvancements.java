package com.oreyield.advancement;

import com.oreyield.OreYieldMod;
import com.oreyield.config.MineralPocketType;
import com.oreyield.loot.MineralPocketResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side awards for Ore Yield's data-driven advancements. */
public final class OreYieldAdvancements {
    private static final String DROP_COUNTER = "ore_yield_drops";
    private static boolean dropCounterRegistered;
    private static final Set<UUID> STONE_GENERATOR_RECIPIENTS = ConcurrentHashMap.newKeySet();
    private static MinecraftServer recipientServer;

    private OreYieldAdvancements() {
    }

    /** Called only after a configured ore drop has been successfully created. */
    public static void onOreDrop(Player player, BlockState hostState, ItemStack drop) {
        if (!(player instanceof ServerPlayer serverPlayer) || drop.isEmpty()) return;

        if (drop.is(Items.DIAMOND)) grant(serverPlayer, "against_all_odds");
        incrementDropCounter(serverPlayer);
    }

    /**
     * Awards the root advancement as soon as a valid, tool-eligible Ore Yield
     * result is selected.  This happens before loaders hand the item to their
     * individual drop-spawning paths, so the root cannot be missed on Fabric.
     */
    public static void onOreYieldRolled(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            grant(serverPlayer, "from_stone");
        }
    }

    /** Called when a player crafts or receives a Stone Generator. */
    public static void onStoneGeneratorObtained(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverFor(serverPlayer);
            if (markStoneGeneratorRecipient(server, serverPlayer.getUUID())) {
                grant(serverPlayer, "the_stone_must_flow");
            }
        }
    }

    /** Avoids running an advancement command every inventory tick for every held generator. */
    private static synchronized boolean markStoneGeneratorRecipient(MinecraftServer server, UUID playerId) {
        if (recipientServer != server) {
            STONE_GENERATOR_RECIPIENTS.clear();
            recipientServer = server;
        }
        return STONE_GENERATOR_RECIPIENTS.add(playerId);
    }

    public static void onMineralPocket(Player player, MineralPocketResult pocket) {
        if (!(player instanceof ServerPlayer serverPlayer) || !pocket.found()) return;

        grant(serverPlayer, "jackpot");
        if (pocket.type() == MineralPocketType.ANCIENT) grant(serverPlayer, "ancient_discovery");
    }

    public static void onBadLuckEliminatorActivated(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            grant(serverPlayer, "very_unlucky_until_now");
        }
    }

    private static void incrementDropCounter(ServerPlayer player) {
        MinecraftServer server = serverFor(player);
        String playerName = player.getScoreboardName();
        if (!dropCounterRegistered) {
            run(server, "scoreboard objectives add " + DROP_COUNTER + " dummy");
            dropCounterRegistered = true;
        }
        run(server, "scoreboard players add " + playerName + " " + DROP_COUNTER + " 1");
        run(server, "execute if score " + playerName + " " + DROP_COUNTER + " matches 1000.. run advancement grant "
                + playerName + " only "
                + OreYieldMod.MOD_ID + ":industrial_revolution");
    }

    private static void grant(ServerPlayer player, String id) {
        MinecraftServer server = serverFor(player);
        // Use Minecraft's own advancement command instead of loader-specific
        // PlayerAdvancements internals. This also sends the normal vanilla
        // advancement update packet to clients that do not have Ore Yield.
        run(server, "advancement grant " + player.getScoreboardName() + " only " + OreYieldMod.MOD_ID + ":" + id);
    }

    private static MinecraftServer serverFor(ServerPlayer player) {
        //? if resourcekey_identifier {
        return player.level().getServer();
        //?} else {
        return player.getServer();
        //?}
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
