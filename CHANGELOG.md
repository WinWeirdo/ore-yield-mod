# Changelog

All notable changes to Ore Yield are documented in this file.

## 1.2.0

### Added

- Stone Generator system with a config-driven recipe and dimension-aware generated host blocks.
- Generator cooldown, reward multiplier, generated-block, player-placement, automation, and explosion controls.
- Curated Create Zinc compatibility.
- Curated Mekanism Tin, Osmium, Uranium, Fluorite, and Lead compatibility.
- Optional Rare Mineral Pockets with configurable category weights, amounts, and multi-resource rewards.
- Redesigned categorized configuration GUI with ore search/filtering, validation, scrolling, staged edits, and localized help.
- Built-in Markdown documentation and a source-generated ore-location reference.

### Changed

- Expanded the configuration GUI and configuration validation.
- Generator Ore Yield is enabled by default; the Mineral Pocket GUI now accepts percentage values from 0 to 100 while TOML remains 0.0 to 1.0.
- Improved native and compatible worldgen replacement for newly generated chunks.
- Improved the compatibility architecture and optional-mod handling.
- Documented default ore Y-levels, peak weighting, chances, drops, Fortune, XP, and mining tiers.

### Fixed

- Fixed modern Minecraft startup on Fabric and NeoForge by assigning registry IDs to custom blocks and the Stone Generator item before construction.
- Deferred compatibility tag scans until the server has fully started, preventing `Tags not bound` startup crashes.

### World generation

- Compatible native worldgen removal affects **newly generated chunks only**. Existing chunks and already-generated ore blocks are not changed.

### Mineral Pockets

- **Rare Mineral Pockets are OFF BY DEFAULT.** Enable `enable_mineral_pockets` explicitly for a modpack or server.

### Migration from 1.1.x

- Existing `config/ore_yield.toml` files load without deletion. Missing 1.2.0 settings receive their shipped defaults in memory.
- `stone_generator_cooldown_ticks` is the canonical generator setting. Older `stone_generator_cooldown` and `stone_generator_interval_ms` values are accepted and converted when loaded; saving rewrites the canonical tick-based setting.
- New optional ore sections and mineral-pocket settings are written when the configuration is next saved. No manual migration is required.
