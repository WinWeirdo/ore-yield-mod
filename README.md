# ore-yield-mod
Minecraft mod for **Forge, Fabric, and NeoForge** that makes ores drop from stone and stone-like blocks.

### Supported Loaders and Versions

| Loader | Supported Minecraft versions |
|---|---|
| Forge | 1.20.1 |
| Fabric | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 |
| NeoForge | 1.21.1, 1.21.11, 26.1.2, 26.2 |

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

### Key Settings

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
