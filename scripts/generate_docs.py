#!/usr/bin/env python3
"""Generate documentation that is derived from Ore Yield's Java defaults.

Usage:
    py -3 scripts/generate_docs.py          rewrite docs/ORE_LOCATIONS.md
    py -3 scripts/generate_docs.py --check  fail when the generated page is stale

This is a development-only utility.  Gameplay never reads generated Markdown.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parent.parent
ORE_CONFIG = ROOT / "template/common/src/main/java/com/oreyield/config/OreConfig.java"
COMPAT_CONFIG = ROOT / "template/common/src/main/java/com/oreyield/compat/ModCompat2Manager.java"
OUTPUT = ROOT / "docs/ORE_LOCATIONS.md"


@dataclass(frozen=True)
class OreDefault:
    entry_id: str
    source: str
    enabled: bool
    dimension: str
    min_y: int
    peak_y: int
    max_y: int
    chance: float
    drop: str
    min_count: int
    max_count: int
    fortune: str
    xp_min: int
    xp_max: int
    pickaxe_level: int


VANILLA_ENTRY = re.compile(
    r'add\(\s*"(?P<id>[^"]+)"\s*,\s*new OreEntry\(\s*"[^"]+"\s*,\s*'
    r'(?P<enabled>true|false)\s*,\s*List\.of\((?P<hosts>\w+)\)\s*,\s*'
    r'"(?P<drop>[^"]+)"\s*,\s*(?P<min_count>\d+)\s*,\s*(?P<max_count>\d+)\s*,\s*'
    r'(?P<chance>[\d.]+)\s*,\s*(?P<min_y>-?\d+)\s*,\s*(?P<max_y>-?\d+)\s*,\s*'
    r'(?P<peak_y>-?\d+)\s*,\s*FortuneType\.(?P<fortune>\w+)\s*,\s*'
    r'(?P<xp_min>\d+)\s*,\s*(?P<xp_max>\d+)\s*,\s*(?P<dimension>\w+)\s*,\s*'
    r'(?P<pickaxe_level>\d+)\s*\)\s*\)\s*;',
    re.MULTILINE,
)

COMPAT_ENTRY = re.compile(
    r'new OreSpec\(\s*"(?P<block>[^"]+)"\s*,\s*"(?P<drop>[^"]+)"\s*,\s*'
    r'(?P<pickaxe_level>\d+)\s*,\s*(?P<dimension>\w+)\s*,\s*(?P<hosts>\w+)\s*,\s*'
    r'(?P<min_count>\d+)\s*,\s*(?P<max_count>\d+)\s*,\s*(?P<chance>[\d.]+)\s*,\s*'
    r'(?P<min_y>-?\d+)\s*,\s*(?P<max_y>-?\d+)\s*,\s*(?P<peak_y>-?\d+)\s*,\s*'
    r'FortuneType\.(?P<fortune>\w+)(?:\s*,\s*(?P<xp_min>\d+)\s*,\s*(?P<xp_max>\d+))?\s*\)',
    re.MULTILINE,
)


SOURCE_NAMES = {
    "minecraft": "Minecraft",
    "create": "Create",
    "mekanism": "Mekanism",
    "iceandfire": "Ice and Fire",
    "simpleores": "SimpleOres",
    "better_tools": "Better Tools",
    "tconstruct": "Tinkers' Construct",
    "netherrocks": "Netherrocks",
    "aether": "The Aether",
    "aether_redux": "Aether Redux",
    "deep_aether": "Deep Aether",
}

DIMENSION_NAMES = {
    "minecraft:overworld": "Overworld",
    "minecraft:the_nether": "Nether",
    "minecraft:the_end": "End",
    "aether:the_aether": "Aether",
}

FORTUNE_NAMES = {
    "NONE": "None",
    "ORE": "Ore multiplier",
    "REDSTONE": "Redstone bonus",
}

PICKAXE_NAMES = {0: "Wood", 1: "Stone", 2: "Iron", 3: "Diamond"}


def java_string_constants(text: str) -> dict[str, str]:
    return {
        name: value
        for name, value in re.findall(r'\bString\s+(\w+)\s*=\s*"([^"]*)"\s*;', text)
    }


def compat_string_constants(text: str) -> dict[str, str]:
    return {
        name: value
        for name, value in re.findall(r'private static final String\s+(\w+)\s*=\s*"([^"]*)"\s*;', text)
    }


def as_default(match: re.Match[str], source: str, constants: dict[str, str]) -> OreDefault:
    values = match.groupdict()
    dimension = constants.get(values["dimension"])
    if dimension is None:
        raise ValueError(f"Unknown dimension constant {values['dimension']!r}")
    return OreDefault(
        entry_id=values.get("id") or entry_id_from_block(values["block"]),
        source=source,
        enabled=values.get("enabled", "true") == "true",
        dimension=dimension,
        min_y=int(values["min_y"]),
        peak_y=int(values["peak_y"]),
        max_y=int(values["max_y"]),
        chance=float(values["chance"]),
        drop=values["drop"],
        min_count=int(values["min_count"]),
        max_count=int(values["max_count"]),
        fortune=values["fortune"],
        xp_min=int(values.get("xp_min") or 0),
        xp_max=int(values.get("xp_max") or 0),
        pickaxe_level=int(values["pickaxe_level"]),
    )


def entry_id_from_block(block_id: str) -> str:
    namespace, path = block_id.split(":", 1)
    material = path.removesuffix("_ore")
    return f"{namespace}:{material}"


def load_defaults() -> list[OreDefault]:
    ore_text = ORE_CONFIG.read_text(encoding="utf-8")
    compat_text = COMPAT_CONFIG.read_text(encoding="utf-8")
    vanilla_constants = java_string_constants(ore_text)
    compat_constants = compat_string_constants(compat_text)

    vanilla = [as_default(match, "Minecraft", vanilla_constants) for match in VANILLA_ENTRY.finditer(ore_text)]
    compat = [
        as_default(match, source_name(match.group("block")), compat_constants)
        for match in COMPAT_ENTRY.finditer(compat_text)
    ]
    if not vanilla or not compat:
        raise ValueError("Could not find the vanilla and curated OreEntry defaults in the Java source")
    return vanilla + compat


def source_name(block_id: str) -> str:
    namespace = block_id.split(":", 1)[0]
    return SOURCE_NAMES.get(namespace, namespace.replace("_", " ").title())


def display_name(entry_id: str) -> str:
    return entry_id.replace(":", " ").replace("_", " ").title()


def chance_text(chance: float) -> str:
    return f"{chance * 100:.7f}".rstrip("0").rstrip(".") + "%"


def range_text(minimum: int, maximum: int, empty: str = "—") -> str:
    if minimum == maximum == 0:
        return empty
    return str(minimum) if minimum == maximum else f"{minimum}–{maximum}"


def row(entry: OreDefault) -> str:
    enabled = "Yes" if entry.enabled and entry.source == "Minecraft" else "Yes, if installed"
    return "| " + " | ".join((
        display_name(entry.entry_id),
        entry.source,
        enabled,
        DIMENSION_NAMES.get(entry.dimension, entry.dimension),
        str(entry.min_y),
        "—" if entry.peak_y == -1 else str(entry.peak_y),
        str(entry.max_y),
        chance_text(entry.chance),
        f"`{entry.drop}`",
        range_text(entry.min_count, entry.max_count, "1"),
        FORTUNE_NAMES[entry.fortune],
        range_text(entry.xp_min, entry.xp_max),
        PICKAXE_NAMES[entry.pickaxe_level],
    )) + " |"


def render(defaults: list[OreDefault]) -> str:
    vanilla = [entry for entry in defaults if entry.source == "Minecraft"]
    curated = [entry for entry in defaults if entry.source != "Minecraft"]
    lines = [
        "<!-- GENERATED FILE: run `py -3 scripts/generate_docs.py` after changing OreConfig.java or ModCompat2Manager.java. -->",
        "# Ore Locations",
        "",
        "This page is generated from Ore Yield's authoritative default ore definitions. Your `config/ore_yield.toml` overrides the values used in-game; this reference shows only the shipped defaults.",
        "",
        "## How to read the table",
        "",
        "- **Chance** is the base chance for one configured ore on each eligible block-break event. Every matching ore entry rolls independently, so more than one resource can drop from one break.",
        "- **Min Y** and **Max Y** are inclusive. A block outside that range cannot roll that entry.",
        "- **Peak Y** applies triangular weighting to the base chance inside the listed range. `—` means `peak_y = -1`: no peak weighting is applied.",
        "- **Amount** is the normal pre-Fortune amount. `Ore multiplier` uses Minecraft-style ore Fortune multiplication; `Redstone bonus` expands the upper amount by the Fortune level; `None` leaves the amount unchanged.",
        "- **XP** is awarded when the entry hits. `—` means no XP. Mining level is the lowest qualifying pickaxe tier.",
        "",
        "## Built-in Minecraft defaults",
        "",
        "| Resource | Source mod | Enabled by default | Dimension | Min Y | Peak Y | Max Y | Chance | Normal drop | Amount | Fortune | XP | Mining level |",
        "| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: | --- | ---: | --- |",
        *(row(entry) for entry in vanilla),
        "",
        "## Curated mod-compatibility defaults",
        "",
        "These entries are enabled by Ore Yield's default `enable_mod_compat_2 = true` setting only when their source mod and configured result item are installed. The installed-mod set therefore determines which rows are active in a given pack.",
        "",
        "| Resource | Source mod | Enabled by default | Dimension | Min Y | Peak Y | Max Y | Chance | Normal drop | Amount | Fortune | XP | Mining level |",
        "| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: | --- | ---: | --- |",
        *(row(entry) for entry in curated),
        "",
        "## Important configuration note",
        "",
        "The table is not a live readout of your server. Edit `[ore.*]` sections in `config/ore_yield.toml` (or use the local config screen) to change real values. Regenerate this page after changing default Java definitions; do not edit the generated table by hand.",
        "",
    ]
    return "\n".join(lines)


def main() -> None:
    check_only = sys.argv[1:] == ["--check"]
    if not check_only and len(sys.argv) != 1:
        raise SystemExit(__doc__)
    try:
        generated = render(load_defaults())
    except (OSError, ValueError) as exc:
        raise SystemExit(f"Cannot generate ore documentation: {exc}") from exc

    current = OUTPUT.read_text(encoding="utf-8") if OUTPUT.exists() else ""
    if current == generated:
        print("docs/ORE_LOCATIONS.md is current")
        return
    if check_only:
        raise SystemExit("docs/ORE_LOCATIONS.md is stale; run: py -3 scripts/generate_docs.py")
    OUTPUT.parent.mkdir(exist_ok=True)
    OUTPUT.write_text(generated, encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
