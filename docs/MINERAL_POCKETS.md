# Mineral Pockets

> **Rare Mineral Pockets are disabled by default.**

Mineral pockets are a separate rare bonus roll after an eligible normal break. They do not replace normal Ore Yield rewards.

## Enabling pockets

Set the master switch in `config/ore_yield.toml`:

```toml
enable_mineral_pockets = true
```

Then tune these settings for the pack:

| Setting | Purpose |
| --- | --- |
| `mineral_pocket_chance` | Chance for the separate pocket roll. The TOML value is 0.0–1.0; the config GUI shows and accepts this as 0–100%. |
| `mineral_pocket_min_resource_types`, `mineral_pocket_max_resource_types` | Number of distinct resources selected by multi-resource pocket categories. |
| `mineral_pocket_<category>_enabled` | Enables Coal, Metal, Precious, Gem, or Ancient. |
| `mineral_pocket_<category>_weight` | Relative selection weight for that category. |
| `mineral_pocket_<category>_min_count`, `mineral_pocket_<category>_max_count` | Amount per selected resource type. |
| `mineral_pockets_end_enabled` | Allows pockets in the End. |
| `mineral_pockets_allow_generator` | Allows pockets from marked Stone Generator output. |
| `mineral_pockets_allow_automated_harvesting` | Allows pockets from automated harvesting. |

Pockets operate in the Overworld and can optionally operate in the End. They intentionally never operate in the Nether.

## Rewards and safeguards

Coal, Precious, and Ancient choose their configured resource. Metal and Gem pockets can select multiple distinct available resources, including supported installed-mod materials where applicable.

Pockets do not advance or consume bad-luck-eliminator counters, and Fortune does not change their amounts. Silk Touch, explosions, blocked automation, blocked generator output, an ineligible host, or an unsupported dimension prevents the pocket roll.
