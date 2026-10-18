"""Which GT6 item-generator flags does *no* material actually carry?

A condition written over such a flag is dead in the original: `dustImpure = DIRTY_DUSTS` for example —
`DIRTY_DUSTS` is declared in `TD.java:577` but never used in `MT.java`, so GT6 registers **no** impure
dusts, while the port generates them for its ore-processing chain (a documented deviation).

Usage:  python tools/report_unused_form_flags.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import collections
import pathlib
import re

FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
ROW = re.compile(r'^\s+"[^"|]+\|([A-Z_,]*)"', re.M)


def main() -> None:
    counts: collections.Counter[str] = collections.Counter()
    for flags in ROW.findall(FORMS.read_text(encoding="utf-8")):
        for flag in flags.split(","):
            if flag:
                counts[flag] += 1
    declared = re.search(r"FLAGS = java\.util\.List\.of\((.*?)\);",
                         FORMS.read_text(encoding="utf-8"), re.S)
    flags = re.findall(r'"([A-Z_]+)"', declared.group(1)) if declared else []
    print(f"flags declared: {len(flags)}")
    unused = [flag for flag in flags if counts.get(flag, 0) == 0]
    rare = [(flag, counts[flag]) for flag in flags if 0 < counts.get(flag, 0) <= 3]
    print(f"flags no material carries (dead in the original): {len(unused)}: {', '.join(unused)}")
    print(f"flags with 1-3 materials: {', '.join(f'{flag}({count})' for flag, count in rare)}")
    print("--- counts for the flags the port's conditions consult ---")
    for flag in ("ORES", "PARTS", "DUSTS", "DIRTY_DUSTS", "PROJECTILES", "PLATES", "STICKS",
                 "GEMS", "INGOTS", "WIRES", "PIPES", "CONTAINERS", "ARMORS", "LENSES", "RAILS",
                 "MULTIPLATES", "DENSEPLATES", "MULTIINGOTS", "INGOTS_HOT", "FOILS", "PLANTS"):
        print(f"  {flag:16} {counts.get(flag, 0)}")


if __name__ == "__main__":
    main()
