# Ore Yield documentation

Ore Yield moves mining rewards from natural ore blocks to configured stone-like host blocks. These pages describe the defaults shipped with the mod and the settings that change them.

| Page | For |
| --- | --- |
| [Getting Started](GETTING_STARTED.md) | Players and first-time pack authors |
| [How Ore Yield Works](HOW_IT_WORKS.md) | Roll rules, height ranges, Fortune, and the bad-luck eliminator |
| [Ore Locations](ORE_LOCATIONS.md) | Generated reference for every built-in and curated default ore |
| [Stone Generators](STONE_GENERATORS.md) | Recipe, output, automation, and generator-specific settings |
| [Mod Compatibility](MOD_COMPATIBILITY.md) | Create, Mekanism, curated ores, and new-chunk worldgen replacement |
| [Configuration](CONFIGURATION.md) | Config file, GUI, ore sections, and custom entries |
| [Mineral Pockets](MINERAL_POCKETS.md) | Optional rare multi-resource bonuses |
| [FAQ and Troubleshooting](FAQ.md) | Common questions and diagnostic steps |

## Keeping the ore reference accurate

`ORE_LOCATIONS.md` is generated from the default entries in `OreConfig.java` and the curated entries in `ModCompat2Manager.java`; it is not read by the mod at runtime.

```powershell
py -3 scripts/generate_docs.py
py -3 scripts/generate_docs.py --check
```

Run the first command whenever those default Java definitions change. The `--check` form is useful in CI or before committing.
