package com.oreyield.config;

/** Categories used by the optional rare mineral-pocket reward roll. */
public enum MineralPocketType {
    COAL("coal", 7, new String[]{"minecraft:coal"}),
    METAL("metal", 12, new String[]{
            "minecraft:raw_iron", "minecraft:raw_copper", "create:raw_zinc",
            "mekanism:raw_tin", "mekanism:raw_lead"
    }),
    PRECIOUS("precious", 20, new String[]{"minecraft:raw_gold"}),
    GEM("gem", 30, new String[]{"minecraft:diamond", "minecraft:emerald", "mekanism:fluorite_gem"}),
    ANCIENT("ancient", 50, new String[]{"minecraft:ancient_debris"});

    private final String configKey;
    private final int defaultExperience;
    private final String[] itemIds;

    MineralPocketType(String configKey, int defaultExperience, String[] itemIds) {
        this.configKey = configKey;
        this.defaultExperience = defaultExperience;
        this.itemIds = itemIds;
    }

    public String configKey() {
        return configKey;
    }

    public int defaultExperience() {
        return defaultExperience;
    }

    public String[] itemIds() {
        return itemIds.clone();
    }
}
