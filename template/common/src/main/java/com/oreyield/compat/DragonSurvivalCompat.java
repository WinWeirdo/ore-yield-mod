package com.oreyield.compat;

import com.oreyield.platform.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional server-safe bridge to Dragon Survival's effective paw harvest check. */
public final class DragonSurvivalCompat {
    private static final String MOD_ID = "dragonsurvival";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ore_yield/DragonSurvivalCompat");
    private static final Accessor ACCESSOR = createAccessor();
    private static final AtomicBoolean INVOCATION_WARNING_LOGGED = new AtomicBoolean();

    private DragonSurvivalCompat() {}

    /**
     * Checks whether the physical loot-context tool or Dragon Survival's effective
     * paw/claw capability can harvest a block. The physical stack remains the sole
     * source of loot enchantments; this method is only a harvest-capability check.
     */
    public static boolean canHarvest(ItemStack lootTool, Player player, BlockState state) {
        if (!lootTool.isEmpty() && lootTool.isCorrectToolForDrops(state)) return true;
        if (player == null) return false;
        try {
            return ACCESSOR.canHarvest(player, state);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (INVOCATION_WARNING_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("[Ore Yield] Dragon Survival harvest compatibility failed; using the loot-context tool only", error);
            }
            return false;
        }
    }

    private static Accessor createAccessor() {
        if (!Services.PLATFORM.isModLoaded(MOD_ID)) return Accessor.UNAVAILABLE;

        try {
            Class<?> providerClass = Class.forName(
                    "by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider");
            Class<?> handlerClass = Class.forName(
                    "by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateHandler");
            Method isDragon = handlerClass.getMethod("isDragon");

            try {
                Method getData = providerClass.getMethod("getData", Player.class);
                Method canHarvest = handlerClass.getMethod("canHarvestWithPaw", Player.class, BlockState.class);
                return (player, state) -> {
                    Object handler = getData.invoke(null, player);
                    return handler != null && Boolean.TRUE.equals(isDragon.invoke(handler))
                            && Boolean.TRUE.equals(canHarvest.invoke(handler, player, state));
                };
            } catch (NoSuchMethodException ignored) {
                // Dragon Survival 1.20.1 exposes the same capability through its
                // Forge capability provider and a one-argument paw harvest method.
                Method getHandler = providerClass.getMethod("getHandler", Entity.class);
                Method canHarvest = handlerClass.getMethod("canHarvestWithPaw", BlockState.class);
                return (player, state) -> {
                    Object handler = getHandler.invoke(null, player);
                    return handler != null && Boolean.TRUE.equals(isDragon.invoke(handler))
                            && Boolean.TRUE.equals(canHarvest.invoke(handler, state));
                };
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            LOGGER.warn("[Ore Yield] Dragon Survival is installed, but its harvest API could not be linked; "
                    + "using the loot-context tool only", error);
            return Accessor.UNAVAILABLE;
        }
    }

    @FunctionalInterface
    private interface Accessor {
        Accessor UNAVAILABLE = (player, state) -> false;

        boolean canHarvest(Player player, BlockState state) throws ReflectiveOperationException;
    }
}
