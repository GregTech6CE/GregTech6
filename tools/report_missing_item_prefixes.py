"""Which GT6 item prefixes have no item form in the port at all?

``MaterialPrefix`` (107 fields) is where the port defines the item forms a material can have; block
forms live in ``MaterialPrefixes``' ``block("…")`` builders. GT6 declares 225 prefixes with a
condition in ``OP.java``. This prints the difference, with the GT6 flags each missing prefix needs, so
the next content batch can be scoped by flag family.

Usage:  python tools/report_missing_item_prefixes.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import pathlib
import re

PREFIX = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java"))
REGISTRY = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefixes.java"))
COMPARISON = pathlib.Path("docs/prefix-condition-comparison.json")

FAMILIES = [
    ("ore processing", r"^(clump|crystal|crystalPure|crystalline|chunk|dustPure|dustRefined|reduced|"
                       r"rubble|pebbles|rawOreChunk|cleanGravel|dirtyGravel|crushed|purified|centrifuged)"),
    ("gem forms", r"^(gemOre|gemRaw|gemUncut|gemPolished)"),
    ("plates/sheets", r"^(sheetGt|sheetDouble|sheet|compressed|plateSteamcraft|plateTiny|plateGem|foil)"),
    ("tools", r"^(tool|toolAxe|toolHoe|toolPickaxe|toolShears|toolShovel|toolSword|weapon)"),
    ("armor", r"^armor"),
    ("wires/cables", r"^(wire|wireGt\d+|cable|cableGt\d+)"),
    ("pipes", r"^(pipe|pipeRestrictive|fluidPipe|itemPipe)"),
    ("containers", r"^(cell|capsule|capcellcon|bottle|bucket|can|drum|tank)"),
    ("machine parts", r"^(casing|frameGt|gear|ring|spring|rotor|motor|pump|piston|robotArm|sensor|emitter|"
                      r"conveyor|circuit|chipset|component)"),
    ("ores", r"^ore"),
    ("food/plants", r"^(plant|seed|berry|fruit|sapling|log|leaves|flower|crop)"),
]


def main() -> None:
    item_fields = set(re.findall(r"MaterialPrefix\s+(\w+)\s*;", PREFIX.read_text(encoding="utf-8")))
    blocks = set(re.findall(r'\bblock\(\s*"(\w+)"', REGISTRY.read_text(encoding="utf-8")))
    conditions = json.loads(COMPARISON.read_text(encoding="utf-8"))["conditions"]

    missing = []
    for name, entry in sorted(conditions.items()):
        if name in item_fields or name in blocks:
            continue
        missing.append((name, entry["gt6Flags"], entry["gt6Expression"]))

    print(f"GT6 prefixes with a condition: {len(conditions)}; port item forms: {len(item_fields)}; "
          f"port block forms: {len(blocks)}")
    print(f"GT6 prefixes with neither an item nor a block form in the port: {len(missing)}")
    claimed: set[str] = set()
    for label, pattern in FAMILIES:
        names = [name for name, _flags, _expr in missing if re.search(pattern, name) and name not in claimed]
        claimed |= set(names)
        if names:
            detail = ", ".join(
                f"{name}[{','.join(flags) if flags else '—'}]"
                for name, flags, _expr in missing if name in names)
            print(f"  {label:16} {len(names):3}  {detail}")
    rest = [(name, flags) for name, flags, _expr in missing if name not in claimed]
    print(f"  {'other':16} {len(rest):3}  " + ", ".join(f"{n}[{','.join(f) if f else '—'}]" for n, f in rest))


if __name__ == "__main__":
    main()
