package com.oreyield.config;

/** Preset frequencies for the independent mineral-pocket reward roll. */
public enum MineralPocketFrequency {
    DEDICATED_MINER("dedicated_miner", 4_147),
    CASUAL_MINER("casual_miner", 2_700),
    NEWBIE_MINER("newbie_miner", 1_600);

    private final String configKey;
    private final int eligibleBreaks;

    MineralPocketFrequency(String configKey, int eligibleBreaks) {
        this.configKey = configKey;
        this.eligibleBreaks = eligibleBreaks;
    }

    public String configKey() {
        return configKey;
    }

    public int eligibleBreaks() {
        return eligibleBreaks;
    }

    public double chance() {
        return 1.0D / eligibleBreaks;
    }

    public static MineralPocketFrequency fromConfigKey(String value) {
        if (value == null) return null;
        String normalized = value.strip();
        for (MineralPocketFrequency frequency : values()) {
            if (frequency.configKey.equalsIgnoreCase(normalized)) return frequency;
        }
        return null;
    }
}
