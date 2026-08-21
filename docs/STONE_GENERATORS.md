# Stone Generators

## Default recipe

The Stone Generator's recipe is config-driven. With the shipped defaults, fill the eight outer slots of a 3×3 crafting grid with diamonds and put an end stone in the center:

```text
Diamond   Diamond   Diamond
Diamond   End Stone Diamond
Diamond   Diamond   Diamond
```

`stone_generator_surrounding_item` and `stone_generator_center_item` change those two ingredients in `config/ore_yield.toml`.

## Operation

The block is server-ticked. After `stone_generator_cooldown_ticks`, it attempts to fill only the air block directly above itself:

- Overworld: stone
- Nether: netherrack
- End: end stone

It never replaces an occupied block, and it has no host-block mapping for custom dimensions. Generated output is persistently marked so Ore Yield can distinguish it from naturally generated and player-placed blocks.

The generator drops itself only when mined with a diamond or netherite pickaxe (and not in creative mode).

## Ore Yield and anti-cheese controls

`generator_ore_yield_enabled` is enabled by default, so marked output rolls normal Ore Yield rewards. Disable it to make the generator produce only its host block. `generator_ore_yield_chance_multiplier` multiplies the matching ore entry's chance only for marked generator output.

The following controls are intentionally separate:

| Setting | Effect |
| --- | --- |
| `allow_player_placed_eligible_blocks` | Allows or blocks Ore Yield on marked player-placed host blocks. |
| `allow_generator_automated_harvesting` | Allows or blocks automated harvesting of marked generator output. |
| `allow_generator_explosion_harvesting` | Allows or blocks explosions from producing Ore Yield rewards on marked generator output. |

Automation and explosion harvesting of generator output are off by default. Mineral pockets have their own generator and automation settings; see [Mineral Pockets](MINERAL_POCKETS.md).

## Cooldown migration

The canonical setting is `stone_generator_cooldown_ticks` (20 ticks = one second). Existing `stone_generator_interval_ms` values are read as a legacy compatibility alias and converted to ticks; new configurations should use the tick-based key.
