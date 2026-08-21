# How Ore Yield Works

## An eligible break

When a configured host block is broken, Ore Yield checks every entry that matches the block and dimension. Each matching entry rolls independently, so one eligible break can produce multiple configured resources. A disabled entry, an out-of-range height, or an insufficient pickaxe cannot produce a normal Ore Yield hit.

The chance shown in [Ore Locations](ORE_LOCATIONS.md) is the entry's base chance for one eligible break. It is not the chance of receiving exactly one item from the entire table.

## Height distribution

`min_y` and `max_y` are inclusive hard limits. `peak_y` changes the chance within that interval:

- A valid peak applies a triangular weighting, with the listed base chance at the peak and lower chances toward the ends of the range.
- `peak_y = -1` disables the weighting; the base chance is uniform throughout the allowed range.
- End overrides from curated compatibility entries deliberately skip height weighting where the entry is configured as an End variant.

## Drops, Fortune, and XP

The normal amount is chosen uniformly from `min_count` through `max_count`.

- `ORE` keeps that normal amount, then may apply Minecraft-style ore Fortune multiplication.
- `REDSTONE` extends the upper amount by the Fortune level.
- `NONE` leaves the normal amount unchanged.

XP is independently chosen from `xp_min` through `xp_max` when an entry hits. A zero range grants no XP.

## Bad Luck Eliminator

The bad-luck eliminator is enabled by default. It tracks failed eligible breaks per player and ore entry. After `ceil(bad_luck_multiplier / adjusted chance)` eligible failures, the next eligible break is forced to drop that entry. A hit resets that entry's counter.

Only a break that could genuinely produce that entry advances its counter. Generator output uses a separate counter and its configured chance multiplier. Disabling `bad_luck_eliminator` restores plain independent rolls.
