package com.oreyield.forge.platform;

import com.oreyield.platform.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;

public class PlatformHelperImpl implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isFakePlayer(Player player) {
        return player instanceof net.minecraftforge.common.util.FakePlayer;
    }
}
