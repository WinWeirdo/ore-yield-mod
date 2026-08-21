# Configuration

Ore Yield reads `config/ore_yield.toml`. The first run writes the available settings and ore sections. The config screen can edit the local file, but it does not send changes to a remote multiplayer server.

## Main settings

| Group | Settings |
| --- | --- |
| Rolls | `bad_luck_eliminator`, `bad_luck_multiplier`, `auto_detect_dimensions`, `enabled_dimensions` |
| World generation | `remove_vanilla_ore_generation`, `remove_compatible_ore_generation`, `enable_vanilla_end_ores` |
| Compatibility | `enable_mod_compat`, `enable_mod_compat_2`, `mod_compat_2_ores_in_end` |
| Stone Generator | `stone_generator_cooldown_ticks`, recipe-item settings, reward multiplier, player-placement and automation/explosion controls |
| Mineral Pockets | master toggle, End/generator/automation toggles, chance, resource-type range, and per-category weights/counts |

See the focused pages for behavior: [How Ore Yield Works](HOW_IT_WORKS.md), [Stone Generators](STONE_GENERATORS.md), [Mod Compatibility](MOD_COMPATIBILITY.md), and [Mineral Pockets](MINERAL_POCKETS.md).

## `[ore.*]` sections

Each default ore can be overridden in its own TOML section. The generated [Ore Locations](ORE_LOCATIONS.md) page shows the defaults; your file is authoritative once changed.

| Field | Meaning |
| --- | --- |
| `enabled` | Turns the entry on or off. |
| `host_blocks` | TOML array of block IDs or host tags, such as `["#forge:overworld_ore_bearing_stones"]`. |
| `result_item` | Registry ID of the reward item. |
| `min_count`, `max_count` | Inclusive normal drop amount. |
| `chance` | Base probability from 0.0 to 1.0 for an eligible break. |
| `min_y`, `max_y`, `peak_y` | Height limits and optional weighting peak; `-1` disables peak weighting. |
| `fortune_type` | `ORE`, `REDSTONE`, or `NONE`. |
| `xp_min`, `xp_max` | Inclusive XP range after a hit. |
| `dimension` | Dimension ID, or empty to allow any dimension. |
| `min_pickaxe_level` | 0 = wood, 1 = stone, 2 = iron, 3 = diamond. |

## Custom entries

`additional_ores` accepts pipe-delimited custom entries. The order is:

```text
id|enabled|result_item|min_count|max_count|chance|min_y|max_y|peak_y|fortune_type|xp_min|xp_max|dimension|host1,host2|min_pickaxe_level
```

Use namespaced registry IDs, include at least one host, and keep `max_count >= min_count`, `max_y >= min_y`, and `xp_max >= xp_min`. The config screen enforces the equivalent validation for editable built-in entries.
