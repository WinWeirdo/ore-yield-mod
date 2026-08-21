package com.oreyield.compat;

import com.oreyield.platform.Services;
import com.oreyield.util.ResourceLocations;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Exact 1.20.1 placed features owned by the supported optional compatibility mods. */
public final class CompatibleOreWorldgen {
    private static final Set<ResourceLocation> CREATE_FEATURES = Set.of(
            ResourceLocations.of("create", "zinc_ore")
    );
    private static final Set<ResourceLocation> MEKANISM_FEATURES = Set.of(
            ResourceLocations.of("mekanism", "ore_tin_small"),
            ResourceLocations.of("mekanism", "ore_tin_large"),
            ResourceLocations.of("mekanism", "ore_osmium_upper"),
            ResourceLocations.of("mekanism", "ore_osmium_middle"),
            ResourceLocations.of("mekanism", "ore_osmium_small"),
            ResourceLocations.of("mekanism", "ore_uranium_small"),
            ResourceLocations.of("mekanism", "ore_uranium_buried"),
            ResourceLocations.of("mekanism", "ore_fluorite_normal"),
            ResourceLocations.of("mekanism", "ore_fluorite_buried"),
            ResourceLocations.of("mekanism", "ore_lead_normal")
    );

    private CompatibleOreWorldgen() {}

    /** Features to remove only when the corresponding optional mod is actually loaded. */
    public static List<ResourceLocation> featuresToRemove() {
        List<ResourceLocation> features = new ArrayList<>();
        if (Services.PLATFORM.isModLoaded("create")) features.addAll(CREATE_FEATURES);
        if (Services.PLATFORM.isModLoaded("mekanism")) features.addAll(MEKANISM_FEATURES);
        return List.copyOf(features);
    }

    public static boolean isTargetFeature(ResourceLocation id) {
        if ("create".equals(id.getNamespace())) {
            return Services.PLATFORM.isModLoaded("create") && CREATE_FEATURES.contains(id);
        }
        if ("mekanism".equals(id.getNamespace())) {
            return Services.PLATFORM.isModLoaded("mekanism") && MEKANISM_FEATURES.contains(id);
        }
        return false;
    }
}
