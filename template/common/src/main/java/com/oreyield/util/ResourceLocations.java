package com.oreyield.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Single construction point for ResourceLocation, covering the version rename chain:
 * pre-1.21 public constructor -> 1.21+ private constructor (fromNamespaceAndPath) -> 26.1+ Identifier.
 */
public final class ResourceLocations {
    public static ResourceLocation of(String namespace, String path) {
        //? if identifier_renamed {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else if resourcelocation_factory_required {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }

    private ResourceLocations() {}
}
