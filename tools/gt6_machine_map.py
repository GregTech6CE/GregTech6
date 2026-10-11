#!/usr/bin/env python3
"""Shared mapping between port machine names/tiers and the original GT6 registrations.

Used by tools/generate_machine_recipe_table.py and tools/generate_machine_params_table.py
so the two generated tables can never disagree about which GT6 registration backs a port
machine variant.
"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "core/src/main/java/com/gregtech/gregtech"

# port machine name -> original GT6 machine name (Basic Machines tab), same tier
SAME_NAME = {
    "oven": "Oven",
    "roaster": "Roasting Oven",
    "distillery": "Distillery",
    "smelter": "Smelter",
    "crystallisationcrucible": "Crystallisation Crucible",
    "dryer": "Dryer",
    "laminator": "Laminator",
    "catalyticcracker": "Catalytic Cracker",
    "steamcracker": "Steam Cracker",
    "shredder": "Shredder",
    "lathe": "Lathe",
    "buzzsaw": "Buzzsaw",
    "centrifuge": "Centrifuge",
    "rollingmill": "Rolling Mill",
    "rollbender": "Roll Bender",
    "rollformer": "Roll Former",
    "clustermill": "Cluster Mill",
    "wiremill": "Wiremill",
    "mixer": "Mixer",
    "loom": "Loom",
    "sluice": "Sluice",
    "sander": "Sanding Machine",
    "burnmixer": "Burner Mixer",
    "debarker": "Pressure Washer",
    "crusher": "Crusher",
    "sifter": "Sifter",
    "squeezer": "Squeezer",
    "compressor": "Compressor",
    "press": "Press",
    "electrolyzer": "Electrolyzer",
    "canner": "Canning Machine",
    "injector": "Injector",
    "printer": "Printer",
    "scannervisuals": "Scanner (Visuals,",
    "autocrafter": "Autocrafter",
    "electricmixer": "Electric Mixer",
    "electricloom": "Electric Loom",
    "electricsifter": "Electric Sifter",
    "slicer": "Slicer",
    "nanofab": "Nanoscale Fabricator",
    "plantalyzer": "Plantalyzer",
    "bumblelyzer": "Bumblelyzer",
    "boxinator": "Boxinator",
    "unboxinator": "Unboxinator",
    "polarizer": "Polarizer",
    "magneticseparator": "Magnetic Separator",
    "autoclave": "Autoclave",
    "bath": "Bath",
    "generifier": "Generifier",
    "coagulator": "Coagulator",
    "fermenter": "Fermenter",
    "melter": "Melter",
    "lightning": "Lightning Processor",
}

# port machine -> "<original name with %d>" ; each port tier is a separate GT6 machine
PER_TIER_NAME = {
    "laserengraver": "Laser Engraver (T%d)",
    "laserwelder": "Laser Welder (T%d)",
    "freezer": "Freezer (T%d)",
    "cryomixer": "Cryo Mixer (T%d)",
    "massfab": "Matter Fabricator (T%d)",
    "scannermolecular": "Molecular Scanner (T%d)",
    "replicator": "Matter Replicator (T%d)",
}

# port machine -> explicit original machine name per port tier
EXPLICIT_TIERS = {
    # GT6 registers the lowest tier as its own machine; the port merged it into one type.
    "extruder": ["Low Heat Extruder", "Extruder", "Extruder", "Extruder"],
}

# extra registered variants beyond BasicMachineDefinitions' tier array
EXTRA_TIERS = {
    "massfab": [1, 2, 3, 4, 5],
    "replicator": [1, 2, 3, 4, 5],
}

# Port tier numbers for machines whose GT6 variants are not simply T1..Tn.
# Loader_MultiTileEntities:1549-1553 registers the Molecular Scanner as T3 only (T1/T2/T4/T5 are
# commented out in the original), and its NBT_INPUT of 512 is exactly RecipeMapScannerMolecular's
# recipe power — the port used to register a T1 scanner with NBT_INPUT 32, whose energy envelope
# (16..64) rejects every scanner recipe. The Matter Replicator is registered for all five tiers
# (Loader_MultiTileEntities:1556-1560).
TIER_NUMBERS = {
    "scannermolecular": [3],
}

# Machines the port registers as single blocks but GT6 registers as multiblocks.
SKIP = {
    "cokeoven", "implosioncompressor", "fusionreactor",
    "cryodistillationtower", "distillationtower",
    "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
    "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
    "largecrusher", "largeshredder", "largesqueezer", "largemassfab",
}

# GT6 machine type + tier -> port machine name (reverse of the tables above), used for
# the machines the port registers as blocks although GT6 has them as multiblocks.
MULTIBLOCK_MACHINE_NAMES = {
    "cokeoven": "Coke Oven",
    "implosioncompressor": "Implosion Compressor",
    "fusionreactor": "Fusion Reactor",
    "distillationtower": "Distillation Tower",
    "cryodistillationtower": "Cryo Distillation Tower",
    "largecentrifuge": "Large Centrifuge",
    "largeelectrolyzer": "Large Electrolyzer",
    "largecoagulator": "Large Coagulator Array",
    "largeautoclave": "Large Autoclave",
    "largebath": "Large Bathing Vat",
    "largemixer": "Large Batch Mixer",
    "largefermenter": "Large Fermenter",
    "largeoven": "Large Electric Oven",
    "largesluice": "Large Sluice",
    "largecrusher": "Large Crusher",
    "largeshredder": "Large Shredder",
    "largesqueezer": "Large Squeezer",
    "largemassfab": "Large Matter Fabricator",
}


def norm(expr: str) -> str:
    e = expr.strip()
    e = re.sub(r"\s+\.", ".", e)
    e = re.sub(r"\.\s*dat", ".dat", e)
    e = re.sub(r"\(\s*", "(", e)
    e = re.sub(r"\s*\)", ")", e)
    e = re.sub(r"\s+", " ", e)
    return e


def port_tier_counts() -> dict[str, int]:
    """Tier count per port machine type, parsed from shared BasicMachineCatalog.java."""
    src = (JAVA / "content/machine/BasicMachineCatalog.java").read_text(encoding="utf-8")
    known = {"HU_TIERS": 4, "RU_KU_TIERS": 4, "EU_MU_LU_CU_TIERS": 5, "SS": 1}
    counts: dict[str, int] = {}
    for m in re.finditer(r'new MachineDef\("([a-z_0-9]+)",\s*"(\w+)",\s*(\w+|new GTMaterial\[\]\{[^}]+\})(?:,\s*\d+)?\)', src):
        name, tier_expr = m.group(1), m.group(3).strip()
        if tier_expr in known:
            counts[name] = known[tier_expr]
        elif "GTMaterial[]{" in tier_expr:
            counts[name] = tier_expr.count("Materials.")
        else:
            raise SystemExit("cannot determine tier count for %s (%r)" % (name, tier_expr))
    if not counts:
        raise SystemExit("No shared MachineDef rows found; refusing to generate empty machine tables")
    return counts


def single_block_variants() -> list[tuple[str, int, str]]:
    """(port machine, port tier, original GT6 machine name) for every recipe-bearing variant."""
    out: list[tuple[str, int, str]] = []
    for port_name, tier_count in port_tier_counts().items():
        if port_name in SKIP:
            continue
        tiers = TIER_NUMBERS.get(port_name, EXTRA_TIERS.get(port_name, list(range(1, tier_count + 1))))
        for tier in tiers:
            if port_name in EXPLICIT_TIERS:
                original = EXPLICIT_TIERS[port_name][tier - 1]
            elif port_name in PER_TIER_NAME:
                original = PER_TIER_NAME[port_name] % tier
            elif port_name in SAME_NAME:
                original = SAME_NAME[port_name]
            else:
                raise SystemExit("no GT6 mapping for port machine %s" % port_name)
            out.append((port_name, tier, original))
    return out


def index_entries(entries: list[dict]) -> dict[str, dict[int, dict]]:
    """Original machine name -> tier -> extracted registration entry."""
    by_machine: dict[str, dict[int, dict]] = {}
    for e in entries:
        m = re.search(r"\[(\d+)\]", e["tier"])
        tier = int(m.group(1)) if m else 1
        by_machine.setdefault(e["machine"], {})[tier] = e
    return by_machine


def lookup(by_machine: dict[str, dict[int, dict]], original: str, tier: int) -> dict | None:
    """Entry for a GT6 machine name; per-tier machines ([T1]) carry no index, so fall back."""
    direct = by_machine.get(original, {}).get(tier)
    if direct is not None:
        return direct
    single = by_machine.get(original, {})
    if len(single) == 1:
        return next(iter(single.values()))
    return None
