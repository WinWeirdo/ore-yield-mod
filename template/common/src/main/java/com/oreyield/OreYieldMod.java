package com.oreyield;

import com.oreyield.compat.DimensionManager;
import com.oreyield.compat.ModCompat2Manager;
import com.oreyield.compat.ModCompatManager;
import com.oreyield.config.OreConfig;
import com.oreyield.config.OreConfigIO;
import com.oreyield.platform.Services;
import net.minecraft.server.MinecraftServer;

public final class OreYieldMod {
    public static final String MOD_ID = "ore_yield";

    private OreYieldMod() {}

    public static void init() {
        OreConfigIO.load(Services.PLATFORM.getConfigDirectory().resolve("ore_yield.toml"));
        if (OreConfig.isModCompatEnabled()) {
            ModCompatManager.scan();
        }
        if (OreConfig.isModCompat2Enabled()) {
            ModCompat2Manager.scan();
        }
        OreConfig.rebuild();
    }

    public static void discoverModdedDimensions(MinecraftServer server) {
        DimensionManager.discover(server);
        if (OreConfig.isModCompatEnabled()) {
            ModCompatManager.discoverModdedDimensions(server);
        }
        if (OreConfig.isModCompat2Enabled()) {
            ModCompat2Manager.discoverModdedDimensions(server);
        }
        OreConfig.rebuild();
    }
}
