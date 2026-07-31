package com.oreyield;

import com.oreyield.compat.ModCompat2Manager;
import com.oreyield.compat.ModCompatManager;
import com.oreyield.config.OreConfig;
import com.oreyield.config.OreConfigIO;
import com.oreyield.platform.Services;
import net.minecraft.server.MinecraftServer;

public final class OreYieldMod {
    public static final String MOD_ID = "ore_yield";
    private static volatile boolean modCompatActive = false;
    private static volatile boolean modCompatActive2 = false;

    private OreYieldMod() {}

    public static void init() {
        OreConfigIO.load(Services.PLATFORM.getConfigDirectory().resolve("ore_yield.toml"));
        if (OreConfig.isModCompatEnabled()) {
            ModCompatManager.scan();
            modCompatActive = true;
        }
        if (OreConfig.isModCompat2Enabled()) {
            ModCompat2Manager.scan();
            modCompatActive2 = true;
        }
        OreConfig.rebuild();
    }

    public static void discoverModdedDimensions(MinecraftServer server) {
        if (modCompatActive) {
            ModCompatManager.discoverModdedDimensions(server);
        }
        if (modCompatActive2) {
            ModCompat2Manager.discoverModdedDimensions(server);
        }
        OreConfig.rebuild();
    }

    public static boolean isModCompatActive() {
        return modCompatActive;
    }

    public static void setModCompatActive(boolean active) {
        modCompatActive = active;
    }

    public static boolean isModCompat2Active() {
        return modCompatActive2;
    }

    public static void setModCompat2Active(boolean active) {
        modCompatActive2 = active;
    }
}
