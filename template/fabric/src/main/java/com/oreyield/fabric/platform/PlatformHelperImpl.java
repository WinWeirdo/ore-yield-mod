package com.oreyield.fabric.platform;

import com.oreyield.platform.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;

public class PlatformHelperImpl implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isFakePlayer(Player player) {
        // Fabric has no loader-wide FakePlayer contract. Do not guess from names.
        return false;
    }
}
