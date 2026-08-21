# FAQ and Troubleshooting

## Why did I not get an ore from a stone block?

Check [Ore Locations](ORE_LOCATIONS.md): the block must match the entry's hosts, be in the right dimension and inclusive Y range, and be mined with the required pickaxe tier. The entry can also be disabled in `config/ore_yield.toml`.

## Can several ores drop from one block?

Yes. Matching ore entries roll independently. See [How Ore Yield Works](HOW_IT_WORKS.md).

## Why are normal ores still generating?

Native worldgen remains on by default. Enable the documented worldgen-removal settings, then explore newly generated chunks. Existing chunks are not rewritten. See [Mod Compatibility](MOD_COMPATIBILITY.md).

## Does changing my client config alter a multiplayer server?

No. The config GUI only writes the local configuration. Change the dedicated server's `config/ore_yield.toml` for server-side mining behavior.

## Why is my Stone Generator not producing ore rewards?

It first needs air directly above it. Generator Ore Yield is enabled by default, but automation and explosions each require their own permission setting. See [Stone Generators](STONE_GENERATORS.md).

## Why did a Mineral Pocket not trigger?

They are disabled by default, have a separate rare chance, and do not run in the Nether. Silk Touch, explosions, and disabled generator or automation permissions also block them. See [Mineral Pockets](MINERAL_POCKETS.md).

## How do I gather useful diagnostics?

Press **Alt+F12** in-game. Ore Yield writes `ore_yield_debug.txt` in the game directory with configured entries, discovered modded ores, and diagnostic information useful for a bug report.
