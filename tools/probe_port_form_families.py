"""Probe where the port registers the form families that look "missing" in the prefix comparison.

The comparison compares ``MaterialPrefix`` (item prefixes) with GT6's ``OP.java``, so families the port
organises elsewhere (block forms, tools, pipes, cables, armor) show up as "GT6-only prefixes" even
though they exist. This prints, per family, the classes/methods in the port that mention it.

Usage:  python tools/probe_port_form_families.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re

SRC = native_path(pathlib.Path("src/main/java"))
FAMILIES = {
    "pipe": r'"pipe|pipeSmall|pipeMedium|pipeLarge|pipeHuge|pipeNonuple|pipeQuadruple',
    "cable": r'"cable|cableGt|cable_0|cable01',
    "armor": r"ArmorItem|armorHelmet|armorChestplate|armorLeggings|armorBoots|ArmorMaterial",
    "ore variants": r'"oreNormal"|"oreNether"|"oreEnd"|"orePoor"|"oreRich"|"oreGravel"|"oreBedrock"',
    "ore processing": r'"clump"|"crystal"|"crystalline"|"chunk"|"dustPure"|"dustRefined"|"crushedPurified"',
    "gem forms": r'"gemRaw"|"gemUncut"|"gemPolished"|"gemOre"',
    "bucket/cell/bottle": r'"bucket"|"cell"|"bottle"|BucketItem',
}


def main() -> None:
    files = list(SRC.rglob("*.java"))
    for label, pattern in FAMILIES.items():
        hits = []
        for path in files:
            text = path.read_text(encoding="utf-8", errors="replace")
            found = re.findall(pattern, text)
            if found:
                hits.append((path.name, len(found)))
        hits.sort(key=lambda item: -item[1])
        total = sum(count for _name, count in hits)
        print(f"{label:20} {total:4} hits in {len(hits):2} files: "
              + ", ".join(f"{name}({count})" for name, count in hits[:5]))


if __name__ == "__main__":
    main()
