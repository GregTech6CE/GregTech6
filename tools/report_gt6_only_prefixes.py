"""Group GT6's prefixes that the port has no counterpart for (from the condition comparison).

``docs/prefix-condition-comparison.json`` lists 122 prefixes GT6 declares in ``OP.java`` that
``MaterialPrefix`` does not have. They are content, not conditions, so this groups them by what they
would need (armor system, block forms, ammo, tools, …) to make the follow-up batch scoped.

Usage:  python tools/report_gt6_only_prefixes.py
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

REPORT = pathlib.Path("docs/prefix-condition-comparison.json")
PORT_BLOCKS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/api/prefix/BlockMaterialPrefix.java"))

GROUPS = [
    ("armor", r"^armor"),
    ("ammo", r"^(arrow|bullet|projectile|round)"),
    ("blocks", r"^block|^crate|^bale|^plank|^slab|^stairs"),
    ("tools/weapons", r"^(tool|weapon|sword|pickaxe|axe|shovel|hoe|hammer|wrench|screwdriver|saw|"
                      r"drill|chainsaw|file|chisel|knife|shears|sense|plow|buzzsaw|mortar|pestle|"
                      r"crowbar|wirecutter|spade|universal)"),
    ("parts", r"^(gear|ring|spring|rotor|casing|shaft|spindle|joint|bearing|pipe|frame|panel|"
              r"plate|stick|bolt|screw|nut|rivet|foil|wire|rail|round|ball|comb|curd|seed)"),
    ("fluids/containers", r"^(cell|bottle|can|bucket|container|drum|tank|canister|barrel)"),
    ("food/plants", r"^(food|plant|seed|sapling|log|leaves|flower|crop|berry|fruit|nut|spice)"),
]


def main() -> None:
    report = json.loads(REPORT.read_text(encoding="utf-8"))
    gt6_only = report["gt6Only"]
    print(f"GT6-only prefixes: {len(gt6_only)}")

    port_blocks = set(re.findall(r"BlockMaterialPrefix\s+(\w+)", PORT_BLOCKS.read_text(encoding="utf-8")))
    print(f"port block-form prefixes (BlockMaterialPrefix fields): {sorted(port_blocks)}")

    claimed: set[str] = set()
    for label, pattern in GROUPS:
        names = [name for name in gt6_only if re.search(pattern, name) and name not in claimed]
        claimed |= set(names)
        if names:
            print(f"  {label:20} {len(names):3}  {', '.join(names[:14])}"
                  f"{' …' if len(names) > 14 else ''}")
    rest = [name for name in gt6_only if name not in claimed]
    print(f"  {'unclassified':20} {len(rest):3}  {', '.join(rest[:40])}"
          f"{' …' if len(rest) > 40 else ''}")


if __name__ == "__main__":
    main()
