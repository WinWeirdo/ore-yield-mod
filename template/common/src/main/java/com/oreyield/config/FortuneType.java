package com.oreyield.config;

public enum FortuneType {
    NONE,
    ORE,
    REDSTONE,
    /** Vanilla amethyst-cluster Fortune distribution: 4/8/12/16 shards. */
    AMETHYST;

    public static FortuneType parse(String value) {
        try {
            return FortuneType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
