package com.oreyield.config;

/** Editable per-category settings for a mineral pocket. Amounts apply to each selected resource type. */
public record MineralPocketSettings(boolean enabled, int weight, int minCount, int maxCount) {
}
