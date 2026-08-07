package com.oreyield.compat;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreConfigIO;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Auto-detects modded dimensions at world load and adds newly found values to
 * {@code enabled_dimensions}, so the overworld ore set drops "like in the
 * overworld" in dimensions such as Twilight Forest, Blue Skies or Ad Astra
 * planets. A persisted discovery history means manually removed dimensions remain
 * pruned, while newly discovered dimensions can still be added automatically.
 */
public final class DimensionManager {
    /** Namespaces whose dimensions are already covered by curated mod_compat_2 entries. */
    private static final Set<String> CURATED_NAMESPACES = Set.of("aether", "aether_redux", "deep_aether");

    private DimensionManager() {}

    public static void discover(MinecraftServer server) {
        if (!OreConfig.isAutoDetectDimensionsEnabled()) return;
        TreeSet<String> detected = new TreeSet<>();
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key : server.levelKeys()) {
            //? if resourcekey_identifier {
            String ns = key.identifier().getNamespace();
            String id = key.identifier().toString();
            //?} else {
            String ns = key.location().getNamespace();
            String id = key.location().toString();
            //?}
            if ("minecraft".equals(ns)) continue;
            if (OreConfig.isModCompat2Enabled() && CURATED_NAMESPACES.contains(ns)) continue;
            detected.add(id);
        }
        TreeSet<String> configured = new TreeSet<>(OreConfig.getEnabledDimensions());
        TreeSet<String> recordedDetected = new TreeSet<>(OreConfig.getAutoDetectedDimensions());
        TreeSet<String> previouslyDetected = new TreeSet<>(recordedDetected);
        // Configs created before auto_detected_dimensions existed may already
        // contain manual pruning. Preserve that selection during one-time
        // migration instead of immediately restoring every detected ID.
        if (recordedDetected.isEmpty() && !configured.isEmpty()) {
            previouslyDetected.addAll(detected);
        }
        // A prior auto-detected ID missing from enabled_dimensions was deliberately
        // removed by the user. Do not silently add it back on the next world load.
        TreeSet<String> pruned = new TreeSet<>(previouslyDetected);
        pruned.removeAll(configured);
        TreeSet<String> additions = new TreeSet<>(detected);
        additions.removeAll(pruned);
        additions.removeAll(configured);
        boolean changed = false;
        if (!additions.isEmpty()) {
            configured.addAll(additions);
            OreConfig.setEnabledDimensions(List.copyOf(configured));
            changed = true;
        }
        if (!detected.equals(recordedDetected)) {
            OreConfig.setAutoDetectedDimensions(List.copyOf(detected));
            changed = true;
        }
        if (changed && OreConfig.configPath() != null) {
            OreConfigIO.save(OreConfig.configPath());
        }
        if (!additions.isEmpty()) {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("ore_yield/Dimensions");
            logger.info("[Ore Yield] Added {} auto-detected extra dimension(s): {}", additions.size(), additions);
        }
    }

    /** True when overworld ores may drop in this dimension (toggle enabled and dimension detected). */
    public static boolean isExtraDimension(String dimension) {
        return OreConfig.isAutoDetectDimensionsEnabled()
                && OreConfig.getEnabledDimensions().contains(dimension);
    }
}
