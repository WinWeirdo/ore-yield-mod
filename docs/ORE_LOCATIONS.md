<!-- GENERATED FILE: run `py -3 scripts/generate_docs.py` after changing OreConfig.java or ModCompat2Manager.java. -->
# Ore Locations

This page is generated from Ore Yield's authoritative default ore definitions. Your `config/ore_yield.toml` overrides the values used in-game; this reference shows only the shipped defaults.

## How to read the table

- **Chance** is the base chance for one configured ore on each eligible block-break event. Every matching ore entry rolls independently, so more than one resource can drop from one break.
- **Min Y** and **Max Y** are inclusive. A block outside that range cannot roll that entry.
- **Peak Y** applies triangular weighting to the base chance inside the listed range. `—` means `peak_y = -1`: no peak weighting is applied.
- **Amount** is the normal pre-Fortune amount. `Ore multiplier` uses Minecraft-style ore Fortune multiplication; `Redstone bonus` expands the upper amount by the Fortune level; `None` leaves the amount unchanged.
- **XP** is awarded when the entry hits. `—` means no XP. Mining level is the lowest qualifying pickaxe tier.

## Built-in Minecraft defaults

| Resource | Source mod | Enabled by default | Dimension | Min Y | Peak Y | Max Y | Chance | Normal drop | Amount | Fortune | XP | Mining level |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: | --- | ---: | --- |
| Coal | Minecraft | Yes | Overworld | 0 | 45 | 320 | 5% | `minecraft:coal` | 1 | Ore multiplier | 0–2 | Wood |
| Iron | Minecraft | Yes | Overworld | -64 | 14 | 320 | 2.4% | `minecraft:raw_iron` | 1 | Ore multiplier | — | Stone |
| Copper | Minecraft | Yes | Overworld | -16 | 43 | 112 | 1.8% | `minecraft:raw_copper` | 2–5 | Ore multiplier | — | Stone |
| Gold | Minecraft | Yes | Overworld | -64 | -18 | 32 | 0.6% | `minecraft:raw_gold` | 1 | Ore multiplier | — | Iron |
| Redstone | Minecraft | Yes | Overworld | -64 | -59 | 16 | 1.3% | `minecraft:redstone` | 4–5 | Redstone bonus | 1–5 | Iron |
| Lapis | Minecraft | Yes | Overworld | -64 | -2 | 64 | 1.1% | `minecraft:lapis_lazuli` | 4–9 | Ore multiplier | 2–5 | Stone |
| Diamond | Minecraft | Yes | Overworld | -64 | -59 | 16 | 0.6% | `minecraft:diamond` | 1 | Ore multiplier | 3–7 | Iron |
| Emerald | Minecraft | Yes | Overworld | -16 | 85 | 320 | 0.3% | `minecraft:emerald` | 1 | Ore multiplier | 3–7 | Iron |
| Nether Quartz | Minecraft | Yes | Nether | 10 | 114 | 117 | 2.4% | `minecraft:quartz` | 1 | Ore multiplier | 2–5 | Wood |
| Nether Gold | Minecraft | Yes | Nether | 10 | 16 | 117 | 1.1% | `minecraft:gold_nugget` | 2–6 | Ore multiplier | — | Wood |
| Ancient Debris | Minecraft | Yes | Nether | 8 | 16 | 119 | 0.2% | `minecraft:ancient_debris` | 1 | None | — | Diamond |
| End Coal | Minecraft | Yes | End | 0 | — | 320 | 5% | `minecraft:coal` | 1 | Ore multiplier | 0–2 | Wood |
| End Iron | Minecraft | Yes | End | 0 | — | 320 | 2.4% | `minecraft:raw_iron` | 1 | Ore multiplier | — | Stone |
| End Copper | Minecraft | Yes | End | 0 | — | 320 | 1.8% | `minecraft:raw_copper` | 2–5 | Ore multiplier | — | Stone |
| End Gold | Minecraft | Yes | End | 0 | — | 320 | 0.6% | `minecraft:raw_gold` | 1 | Ore multiplier | — | Iron |
| End Redstone | Minecraft | Yes | End | 0 | — | 320 | 1.3% | `minecraft:redstone` | 4–5 | Redstone bonus | 1–5 | Iron |
| End Lapis | Minecraft | Yes | End | 0 | — | 320 | 1.1% | `minecraft:lapis_lazuli` | 4–9 | Ore multiplier | 2–5 | Stone |
| End Diamond | Minecraft | Yes | End | 0 | — | 320 | 0.6% | `minecraft:diamond` | 1 | Ore multiplier | 3–7 | Iron |
| End Emerald | Minecraft | Yes | End | 0 | — | 320 | 0.3% | `minecraft:emerald` | 1 | Ore multiplier | 3–7 | Iron |
| End Nether Quartz | Minecraft | Yes | End | 0 | — | 320 | 2.4% | `minecraft:quartz` | 1 | Ore multiplier | 2–5 | Wood |
| End Nether Gold | Minecraft | Yes | End | 0 | — | 320 | 1.1% | `minecraft:gold_nugget` | 2–6 | Ore multiplier | — | Wood |

## Curated mod-compatibility defaults

These entries are enabled by Ore Yield's default `enable_mod_compat_2 = true` setting only when their source mod and configured result item are installed. The installed-mod set therefore determines which rows are active in a given pack.

| Resource | Source mod | Enabled by default | Dimension | Min Y | Peak Y | Max Y | Chance | Normal drop | Amount | Fortune | XP | Mining level |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: | --- | ---: | --- |
| Create Zinc | Create | Yes, if installed | Overworld | -63 | — | 70 | 1.5% | `create:raw_zinc` | 1 | Ore multiplier | — | Stone |
| Mekanism Tin | Mekanism | Yes, if installed | Overworld | -32 | 20 | 94 | 3% | `mekanism:raw_tin` | 1 | Ore multiplier | — | Wood |
| Mekanism Osmium | Mekanism | Yes, if installed | Overworld | -64 | 208 | 320 | 3.5% | `mekanism:raw_osmium` | 1 | Ore multiplier | — | Wood |
| Mekanism Uranium | Mekanism | Yes, if installed | Overworld | -64 | -40 | 8 | 1.1% | `mekanism:raw_uranium` | 1 | Ore multiplier | — | Wood |
| Mekanism Fluorite | Mekanism | Yes, if installed | Overworld | -64 | -30 | 23 | 1% | `mekanism:fluorite_gem` | 2–4 | Ore multiplier | 1–4 | Wood |
| Mekanism Lead | Mekanism | Yes, if installed | Overworld | -64 | -12 | 64 | 1.4% | `mekanism:raw_lead` | 1 | Ore multiplier | — | Wood |
| Iceandfire Silver | Ice and Fire | Yes, if installed | Overworld | -16 | — | 112 | 2% | `iceandfire:raw_silver` | 1 | Ore multiplier | — | Stone |
| Simpleores Adamantium | SimpleOres | Yes, if installed | Overworld | 1 | — | 84 | 2.34375% | `simpleores:raw_adamantium` | 1 | Ore multiplier | — | Iron |
| Simpleores Tin | SimpleOres | Yes, if installed | Overworld | 24 | 172 | 236 | 3.7% | `simpleores:raw_tin` | 2–5 | Ore multiplier | — | Stone |
| Simpleores Mythril | SimpleOres | Yes, if installed | Overworld | 1 | — | 96 | 3.125% | `simpleores:raw_mythril` | 1 | Ore multiplier | — | Iron |
| Better Tools Ruby | Better Tools | Yes, if installed | Overworld | -64 | -24 | 16 | 3.90625% | `better_tools:ruby` | 2–5 | Ore multiplier | 3–7 | Stone |
| Better Tools Sapphire | Better Tools | Yes, if installed | Overworld | 100 | 180 | 260 | 3.90625% | `better_tools:sapphire` | 1 | Ore multiplier | — | Iron |
| Better Tools Topaz | Better Tools | Yes, if installed | Overworld | 0 | 60 | 120 | 1.4% | `better_tools:topaz` | 2–5 | Ore multiplier | 2–5 | Iron |
| Simpleores Onyx | SimpleOres | Yes, if installed | Nether | 10 | — | 117 | 1% | `simpleores:onyx_gem` | 1 | Ore multiplier | — | Diamond |
| Better Tools Nether Diamond | Better Tools | Yes, if installed | Nether | 10 | — | 117 | 2.5% | `better_tools:nether_diamond` | 1 | Ore multiplier | 3–7 | Iron |
| Tconstruct Cobalt | Tinkers' Construct | Yes, if installed | Nether | 10 | — | 117 | 1.15% | `tconstruct:raw_cobalt` | 1 | Ore multiplier | — | Iron |
| Netherrocks Argonite | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.5% | `netherrocks:raw_argonite` | 1 | Ore multiplier | — | Diamond |
| Netherrocks Ashstone | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.5% | `netherrocks:ashstone_gem` | 1 | Ore multiplier | — | Diamond |
| Netherrocks Dragonstone | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.25% | `netherrocks:dragonstone_gem` | 1 | Ore multiplier | — | Diamond |
| Netherrocks Fyrite | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.5% | `netherrocks:raw_fyrite` | 1 | Ore multiplier | — | Iron |
| Netherrocks Illumenite | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.5% | `netherrocks:raw_illumenite` | 1 | Ore multiplier | — | Iron |
| Netherrocks Malachite | Netherrocks | Yes, if installed | Nether | 10 | — | 117 | 1.5% | `netherrocks:raw_malachite` | 1 | Ore multiplier | — | Iron |
| Better Tools End Titanium | Better Tools | Yes, if installed | End | 0 | — | 320 | 0.8% | `better_tools:end_titanium_ore` | 1 | None | — | Diamond |
| Aether Gravitite | The Aether | Yes, if installed | Aether | 0 | — | 320 | 1% | `aether_redux:raw_gravitite` | 1 | Ore multiplier | — | Iron |
| Aether Zanite | The Aether | Yes, if installed | Aether | 0 | — | 320 | 1% | `aether:zanite_gemstone` | 1 | Ore multiplier | 2–5 | Stone |
| Aether Ambrosium | The Aether | Yes, if installed | Aether | 0 | — | 320 | 5% | `aether:ambrosium_shard` | 1 | Ore multiplier | 0–2 | Wood |
| Aether Redux Sentrite | Aether Redux | Yes, if installed | Aether | 0 | — | 320 | 3% | `aether_redux:sentrite` | 1 | Ore multiplier | — | Wood |
| Deep Aether Skyjade | Deep Aether | Yes, if installed | Aether | 0 | — | 320 | 2% | `deep_aether:skyjade` | 1 | Ore multiplier | — | Iron |
| Aether Redux Veridium | Aether Redux | Yes, if installed | Aether | 0 | — | 320 | 2% | `aether_redux:raw_veridium` | 1 | Ore multiplier | — | Stone |
| Aether Gravitite | The Aether | Yes, if installed | End | 0 | — | 320 | 1% | `aether_redux:raw_gravitite` | 1 | Ore multiplier | — | Iron |
| Aether Zanite | The Aether | Yes, if installed | End | 0 | — | 320 | 1% | `aether:zanite_gemstone` | 1 | Ore multiplier | 2–5 | Stone |
| Aether Ambrosium | The Aether | Yes, if installed | End | 0 | — | 320 | 5% | `aether:ambrosium_shard` | 1 | Ore multiplier | 0–2 | Wood |
| Aether Redux Sentrite | Aether Redux | Yes, if installed | End | 0 | — | 320 | 3% | `aether_redux:sentrite` | 1 | Ore multiplier | — | Wood |
| Deep Aether Skyjade | Deep Aether | Yes, if installed | End | 0 | — | 320 | 2% | `deep_aether:skyjade` | 1 | Ore multiplier | — | Iron |
| Aether Redux Veridium | Aether Redux | Yes, if installed | End | 0 | — | 320 | 2% | `aether_redux:raw_veridium` | 1 | Ore multiplier | — | Stone |

## Important configuration note

The table is not a live readout of your server. Edit `[ore.*]` sections in `config/ore_yield.toml` (or use the local config screen) to change real values. Regenerate this page after changing default Java definitions; do not edit the generated table by hand.
