# Getting Started

Ore Yield makes configured stone-like blocks produce resources when mined. The exact locations, chances, heights, drops, Fortune behavior, and tool requirements are listed in [Ore Locations](ORE_LOCATIONS.md).

1. Install Ore Yield on a supported loader and Minecraft version, then create or load a world.
2. Mine an eligible host block in the correct dimension and height range with a suitable pickaxe.
3. Use [Ore Locations](ORE_LOCATIONS.md) to choose where to mine. No external scanner is required.
4. Configure the mod from `config/ore_yield.toml` or the config screen. See [Configuration](CONFIGURATION.md).

## Config screen and servers

Forge and NeoForge expose **Ore Yield Config** on the title screen. On Fabric, install Mod Menu and open the config from Ore Yield's mod-list entry.

The config screen edits only the local machine's TOML file; it does not transmit configuration to a multiplayer server. On a dedicated server, change that server's `config/ore_yield.toml`. Players need the server's active configuration for its mining behavior to change.

## Before enabling worldgen replacement

By default, native worldgen remains enabled, so Ore Yield adds possible rewards to eligible mining rather than replacing natural ores. Enabling worldgen removal affects only newly generated chunks. Read [Mod Compatibility](MOD_COMPATIBILITY.md) before using it in an existing world or pack.
