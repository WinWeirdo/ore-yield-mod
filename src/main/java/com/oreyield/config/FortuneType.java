package com.oreyield.config;

public enum FortuneType {
    NONE,
    ORE,
    REDSTONE;

    public static FortuneType parse(String value) {
        try {
            return FortuneType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
