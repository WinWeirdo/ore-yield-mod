package com.oreyield.config;

/** Categories used by the optional rare mineral-pocket reward roll. */
public enum MineralPocketType {
    COAL("coal", new String[]{"minecraft:coal"}),
    METAL("metal", new String[]{
            "minecraft:raw_iron", "minecraft:raw_copper", "create:raw_zinc",
            "mekanism:raw_tin", "mekanism:raw_lead"
    }),
    PRECIOUS("precious", new String[]{"minecraft:raw_gold"}),
    GEM("gem", new String[]{"minecraft:diamond", "minecraft:emerald", "mekanism:fluorite_gem"}),
    ANCIENT("ancient", new String[]{"minecraft:ancient_debris"});

    private final String configKey;
    private final String[] itemIds;

    MineralPocketType(String configKey, String[] itemIds) {
        this.configKey = configKey;
        this.itemIds = itemIds;
    }

    public String configKey() {
        return configKey;
    }

    public String[] itemIds() {
        return itemIds.clone();
    }
}
