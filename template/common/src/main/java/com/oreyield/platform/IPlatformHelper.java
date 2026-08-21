package com.oreyield.platform;

import java.nio.file.Path;
import net.minecraft.world.entity.player.Player;

public interface IPlatformHelper {
    String getPlatformName();

    boolean isModLoaded(String modId);

    Path getConfigDirectory();

    /** True only when the active loader exposes a trustworthy fake-player type. */
    boolean isFakePlayer(Player player);
}
