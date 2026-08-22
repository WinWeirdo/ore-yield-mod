# ore-yield-mod
Minecraft mod for **Forge, Fabric, and NeoForge** that makes ores drop from stone and stone-like blocks.

### Supported Loaders and Versions

| Loader | Supported Minecraft versions |
|---|---|
| Forge | 1.20.1 |
| Fabric | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 |
| NeoForge | 1.21.1, 1.21.11, 26.1.2, 26.2 |

### Documentation

Player and modpack-author documentation lives in [docs/README.md](docs/README.md). The complete, source-generated default reference is [docs/ORE_LOCATIONS.md](docs/ORE_LOCATIONS.md).

Release notes and migration guidance are in [CHANGELOG.md](CHANGELOG.md).

### Basic Concepts

Ore Yield replaces vanilla ore worldgen. Instead of finding ore blocks in walls, you mine normal stone-like blocks and receive ore drops based on probability.

**How it works:**
1. Break a stone block (stone, deepslate, tuff, etc.)
2. Each configured ore rolls independently against its chance
3. Matching ores are added to the loot table (with Fortune applied)
4. XP is awarded for each ore that hits

### Config File Location

```
 config/ore_yield.toml
```

The config screen is available from the Forge and NeoForge title screens ("Ore Yield Config" button). On Fabric, install [Mod Menu](https://modrinth.com/mod/modmenu) to open the config screen from the mod list.

Existing `ore_yield.toml` files are migrated automatically: your configured values stay intact, while newly added settings and built-in ore sections are written into the file for server-side editing. Unrecognized legacy fields and custom ore sections are retained.

### Server-side compatibility

Ore Yield is server-side by default: players can join without installing the mod.

Do **not** enable the following setting unless every client also has the matching Ore Yield version installed:

```toml
stone_generator_enabled = true
# Optional, separate player-placed-block provenance tracking:
enable_anti_cheese_mechanics = true
```

Either setting registers custom blocks that Minecraft must synchronize to clients. Restart the server and all modded clients after changing either setting. Leave both `false` to keep Ore Yield client-optional.

### Key Settings

### Stone Generator

The Stone Generator is available when `stone_generator_enabled = true`, independently of `enable_anti_cheese_mechanics`. It is crafted with eight diamonds around one end stone. After each cooldown, it attempts to create a host block immediately above itself: normal stone in the Overworld, netherrack in the Nether, and end stone in the End. It never replaces an occupied block. Enable Generator Tracking Blocks only when its output needs to be persistently marked and balanced independently.

It explicitly drops itself only when mined with a **diamond or netherite pickaxe**. Other tools do not drop the block.

The cooldown is editable in the config GUI and in the config file. Recipe ingredients remain file-only:

```toml
# Default: one server tick (20 ticks = one second)
stone_generator_cooldown_ticks = 1
# Default: false. Enables the Stone Generator block and recipe; requires Ore Yield on all clients.
stone_generator_enabled = false
# Default: false. Enables custom generated-block tracking. Disabled means normal vanilla Generator output.
generator_tracking_blocks_enabled = false
# Default: false. Enables player-placed anti-cheese markers; requires Ore Yield on all clients.
enable_anti_cheese_mechanics = false
# Default: true. Allows Ore Yield drops from marked generator output.
generator_ore_yield_enabled = true
# Default: 0.25. Applied only when the preceding option is true.
generator_ore_yield_chance_multiplier = 0.25
# Default: true. Controls Ore Yield rolls on marked player-placed host blocks.
allow_player_placed_eligible_blocks = true
# Defaults: false. Protect marked generator output from automation and explosions.
allow_generator_automated_harvesting = false
allow_generator_explosion_harvesting = false
stone_generator_surrounding_item = "minecraft:diamond"
stone_generator_center_item = "minecraft:end_stone"
```

For compatibility, existing `stone_generator_interval_ms` values are still read and converted to ticks. Invalid values fall back to one tick.

Forge and NeoForge recognize their loader-provided fake-player classes. Fabric does not provide a loader-wide equivalent, so its non-player destruction path is treated as automation; this safely blocks generator output by default, but enabling Fabric automated harvesting also permits non-player destruction that cannot be classified more precisely.

### Mineral pockets

Mineral pockets are optional rare bonus drops, disabled by default. They are a separate roll after a successful eligible block break: they never advance or use the Bad Luck Eliminator, Fortune never changes their amounts, and explosions never produce them. Pockets work in the Overworld and optionally in the End; the Nether is intentionally excluded.

```toml
enable_mineral_pockets = false
mineral_pockets_end_enabled = true
mineral_pockets_allow_generator = false
mineral_pockets_allow_automated_harvesting = false

# About one pocket per 4,147 eligible breaks when all five defaults are enabled.
mineral_pocket_chance = 0.00024112121212121212
# Metal and gem pockets choose 2–3 installed resource types. The amount below is per type.
mineral_pocket_min_resource_types = 2
mineral_pocket_max_resource_types = 3

mineral_pocket_coal_enabled = true
mineral_pocket_coal_weight = 4147
mineral_pocket_coal_min_count = 20
mineral_pocket_coal_max_count = 44

mineral_pocket_metal_enabled = true
mineral_pocket_metal_weight = 2765
mineral_pocket_metal_min_count = 7
mineral_pocket_metal_max_count = 18

mineral_pocket_precious_enabled = true
mineral_pocket_precious_weight = 1885
mineral_pocket_precious_min_count = 4
mineral_pocket_precious_max_count = 9

mineral_pocket_gem_enabled = true
mineral_pocket_gem_weight = 1037
mineral_pocket_gem_min_count = 3
mineral_pocket_gem_max_count = 8

mineral_pocket_ancient_enabled = true
mineral_pocket_ancient_weight = 166
mineral_pocket_ancient_min_count = 1
mineral_pocket_ancient_max_count = 2
```

The category weights preserve the intended approximate per-break rates: Coal 1/10,000; Metal 1/15,000; Precious 1/22,000; Gem 1/40,000; Ancient 1/250,000. Metal pools use raw iron and copper plus installed Create Zinc and Mekanism Tin/Lead. Gem pools use diamond and emerald plus installed Mekanism Fluorite. A pocket announces itself after its drops are awarded.

To replace native compatible ores in **new chunks only**, enable vanilla worldgen removal and leave the compatibility safeguard enabled:

```toml
remove_vanilla_ore_generation = true
remove_compatible_ore_generation = true
```

Existing chunks and already-generated ore blocks are unaffected.

#### `remove_vanilla_ore_generation` (default: false)
- `false` — vanilla ores still generate naturally AND stone blocks yield extra drops
- `true` — vanilla ore generation is removed, only stone-drop system remains

#### `enable_vanilla_end_ores` (default: true)
- Adds `end_*` variants of the built-in ores (coal, iron, copper, gold, redstone, lapis, diamond, emerald, nether quartz, nether gold) that drop from end stone in the End
- `false` — end stone yields nothing from the vanilla ore set

#### `bad_luck_eliminator` (default: true)
- Guarantees each ore eventually drops: after N eligible blocks without a hit, the next eligible block **must** drop that ore
- Guarantee window: `ceil(bad_luck_multiplier / chance)` eligible blocks
  - 5% chance + multiplier 2.0 → guaranteed drop after at most 40 blocks
  - 2% chance + multiplier 2.0 → guaranteed drop after at most 100 blocks
- Counters are tracked **per player** and reset on a successful drop or config reload
- Only eligible breaks count (correct dimension, Y range, pickaxe level, not creative, no silk touch)
- `false` — disables pity, plain independent rolls (default behaviour of previous versions)

#### `bad_luck_multiplier` (default: 2.0)
- Multiplier for the bad luck eliminator guarantee window (see above); higher = rarer forced drops, clamped to at least 1.0

#### `enable_mod_compat` (default: false)
- Auto-detects all modded ore blocks and stone blocks at startup
- Detected ores become configurable entries (disabled by default)
- Requires restart

#### `enable_mod_compat_2` (default: true)
- Enables a curated set of ores from specific mods:
  - iceandfire, simpleores, better_tools, tconstruct, netherrocks
  - aether, aether_redux, deep_aether (Aether dimension ores + End variants)
- Only listed ores are enabled (no auto-detection noise)
- Requires restart

#### `mod_compat_2_ores_in_end` (default: true)
- When true, compat2 ores configured for overworld/nether also drop from end stone in the End
- Ancient debris is always excluded from End drops

#### `auto_detect_dimensions` (default: true)
- On world load, detects available dimensions and adds modded ones (e.g. Twilight Forest, Blue Skies, Ad Astra planets, Moon, Venus) to the `enabled_dimensions` list
- In those dimensions, the overworld ore set drops from stone-like blocks just like in the overworld (same Y ranges, chances, and bad luck eliminator)
- Skips vanilla dimensions and curated mod_compat_2 dimensions (Aether and friends) so their own ores are not affected
- `false` — no dimension detection; ores only drop in the dimensions listed per entry
- New dimensions are detected on the next world load after installing a dimension mod

#### `enabled_dimensions` (default: empty)
- List of extra dimensions where the overworld ore set also drops, e.g. `enabled_dimensions = ["twilight_forest:twilight_forest", "ad_astra:moon"]`
- Populated automatically by `auto_detect_dimensions`; you can also edit it manually to prune or pin entries
- Ignored while `auto_detect_dimensions` is disabled

### Built-in Ore Entries (`[ore.*]` sections in `config/ore_yield.toml`)

Each ore has these fields:
| Field | Description |
|-------|-------------|
| `enabled` | Toggle this ore on/off |
| `host_blocks` | Which blocks can yield this ore (supports tags like `#forge:overworld_ore_bearing_stones`) |
| `result_item` | Item dropped |
| `min_count` / `max_count` | Drop count range |
| `chance` | Probability per block break (0.0 - 1.0) |
| `min_y` / `max_y` | Y-level range where this ore can drop |
| `peak_y` | Y-level with highest chance (-1 to disable peak weighting) |
| `fortune_type` | `ORE` (multiplicative), `REDSTONE` (additive), or `NONE` |
| `xp_min` / `xp_max` | XP range awarded per hit |
| `dimension` | Restrict to `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`, or empty for any |
| `min_pickaxe_level` | Minimum pickaxe tier: 0=wood, 1=stone, 2=iron, 3=diamond |

### Adding Custom Ores (additional_ores)

Add entries to the `additional_ores` list in the TOML config. Format is pipe-delimited:

```
id|enabled|result_item|min_count|max_count|chance|min_y|max_y|peak_y|fortune_type|xp_min|xp_max|dimension|host1,host2|min_pickaxe_level
```

**Example — Add a modded copper ore:**
```
mymod:copper|true|mymod:raw_copper|2|5|0.02|-16|112|43|ORE|0|0|minecraft:overworld|#forge:overworld_ore_bearing_stones|0
```

**Field reference:**
- `id` — unique name for this entry
- `enabled` — `true` or `false`
- `result_item` — item registry name
- `min_count`/`max_count` — drop amount range
- `chance` — `0.02` = 2% chance per block break
- `min_y`/`max_y` — Y-level range
- `peak_y` — Y with peak chance, `-1` for no peak
- `fortune_type` — `ORE`, `REDSTONE`, or `NONE`
- `xp_min`/`xp_max` — XP range
- `dimension` — dimension ID or empty for any
- `host_blocks` — comma-separated block IDs or tag references (`#forge:...`)
- `min_pickaxe_level` — `0` to `3`

### Debug Tool

Press **Alt+F12** in-game to scan all registered ore blocks and write debug info to `ore_yield_debug.txt` in your game directory. Shows which ores are configured vs missing, full config dump, and detected modded ores.

### Host Block Tags

The mod provides these tags for use in `host_blocks`:
- `#forge:ore_bearing_stones` — all stone types (overworld + nether + end)
- `#forge:overworld_ore_bearing_stones` — stone, deepslate, tuff, andesite, granite, diorite, calcite
- `#forge:nether_ore_bearing_stones` — netherrack, blackstone, basalt, smooth_basalt

### Supported Mods and Ores (`mod_compat_2`)

The following mods are currently supported by `mod_compat_2`:

* **[Create](https://www.curseforge.com/minecraft/mc-mods/create)**

  * Zinc

* **[Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism)**

  * Tin
  * Osmium
  * Uranium
  * Fluorite
  * Lead

* **[Ice and Fire](https://www.curseforge.com/minecraft/mc-mods/ice-and-fire-dragons)**

  * Silver

* **[Better Tools and Armor](https://www.curseforge.com/minecraft/mc-mods/better-tools-and-armor)**

  * Ruby
  * Sapphire
  * Topaz
  * End Titanium

* **[Simple Ores](https://www.curseforge.com/minecraft/mc-mods/simpleores)**

  * Adamantium
  * Tin
  * Mythril
  * Onyx

* **[Netherrocks](https://www.curseforge.com/minecraft/mc-mods/netherrocks)**

  * Argonite
  * Ashstone
  * Dragonstone
  * Fyrite
  * Illumenite
  * Malachite

* **[Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct)**

  * Cobalt

* **[The Aether](https://www.curseforge.com/minecraft/mc-mods/aether) / [Aether Redux](https://www.curseforge.com/minecraft/mc-mods/aether-redux) / [Deep Aether](https://www.curseforge.com/minecraft/mc-mods/deep-aether)**

  * Gravitite
  * Zanite
  * Ambrosium
  * Sentrite
  * Skyjade
  * Veridium

### Development

- [docs/BUILDING.md](docs/BUILDING.md) — how to generate and build every supported MC version (including the standalone 26.x workflow)
- [docs/ADDING_A_VERSION.md](docs/ADDING_A_VERSION.md) — how to add a new Minecraft version to the matrix
- [docs/VERSION_MATRIX.md](docs/VERSION_MATRIX.md) — auto-generated version/flag matrix
