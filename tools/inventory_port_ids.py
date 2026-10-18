#!/usr/bin/env python3
"""Inventory the port's registered GT item/block ids from the language file.

Used to resolve GT6 crafting-recipe keys (OP.gearGt, IL.PUMPS[3], MT.DATA.CABLES_01[2], ...)
onto actual 1.20.1 registry ids.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import argparse
import collections
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = native_path(ROOT / "src/main/resources/assets/gregtech/lang/en_us.json")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("patterns", nargs="*", default=[""], help="substring filters")
    ap.add_argument("--blocks", action="store_true", help="include block ids")
    ap.add_argument("--limit", type=int, default=40)
    args = ap.parse_args()

    data = json.loads(LANG.read_text(encoding="utf-8"))
    items = sorted(k.split(".", 2)[2] for k in data if k.startswith("item.gregtech."))
    blocks = sorted(k.split(".", 2)[2] for k in data if k.startswith("block.gregtech."))
    pool = items + (blocks if args.blocks else [])

    for pat in args.patterns:
        hits = [i for i in pool if pat.lower() in i.lower() and ".tooltip" not in i]
        print(f"== {pat!r}: {len(hits)} ==")
        for h in hits[: args.limit]:
            print("   ", h)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
