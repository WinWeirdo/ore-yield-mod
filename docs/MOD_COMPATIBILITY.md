# Mod Compatibility

## Curated compatibility

`enable_mod_compat_2` is enabled by default. It activates curated entries only when the relevant mod and result item are present. The complete generated list is in [Ore Locations](ORE_LOCATIONS.md).

The primary built-in integrations are:

| Mod | Resources |
| --- | --- |
| Create | Zinc |
| Mekanism | Tin, Osmium, Uranium, Fluorite, Lead |

These resources keep their authored Overworld profiles. Ore Yield does not create separate End profiles for the Create and Mekanism integration set.

The curated list also includes optional entries for the other supported compatibility mods. Their entries are only active when their respective mods are installed.

## Replacing native compatible worldgen

To replace native ore generation in newly generated chunks, enable both settings:

```toml
remove_vanilla_ore_generation = true
remove_compatible_ore_generation = true
```

For the verified Create and Mekanism entries, this removes their native placed features from new chunks and makes the configured resources available through Ore Yield instead. It does not rewrite old chunks, remove already-generated ore blocks, or make existing ore blocks unmineable; those blocks retain their ordinary mining behavior.

`remove_compatible_ore_generation` is a safeguard for compatible entries. Keep it enabled when using worldgen replacement unless the pack intentionally wants native compatible ore features to remain.

## Generic automatic detection

`enable_mod_compat` is a separate, opt-in generic scanner for modded ores and stone-like hosts. Its discovered entries are disabled by default and require a restart because registry scanning happens during startup. Use it when a pack needs a mod not represented by the curated set.
