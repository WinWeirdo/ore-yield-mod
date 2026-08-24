package com.oreyield.config;

/** Categories used by the optional rare mineral-pocket reward roll. */
public enum MineralPocketType {
    COAL("coal", 3, new String[]{"minecraft:coal"}),
    METAL("metal", 5, new String[]{
            "minecraft:raw_iron", "minecraft:raw_copper", "create:raw_zinc",
            "mekanism:raw_tin", "mekanism:raw_lead"
    }),
    PRECIOUS("precious", 8, new String[]{"minecraft:raw_gold"}),
    GEM("gem", 12, new String[]{"minecraft:diamond", "minecraft:emerald", "mekanism:fluorite_gem"}),
    ANCIENT("ancient", 20, new String[]{"minecraft:ancient_debris"});

    private final String configKey;
    private final int experience;
    private final String[] itemIds;

    MineralPocketType(String configKey, int experience, String[] itemIds) {
        this.configKey = configKey;
        this.experience = experience;
        this.itemIds = itemIds;
    }

    public String configKey() {
        return configKey;
    }

    public int experience() {
        return experience;
    }

    public String[] itemIds() {
        return itemIds.clone();
    }
}
