"""Report which survival items have no recipe at all (datapack or machine).

Written as a standalone script so the check can run without a game instance; the same numbers are
produced in-game by `gametest/CraftingCoverageTests` (which also writes
`docs/items-without-recipes.json`). The two exist because the in-game version resolves real item
objects while this one works from the recipe JSON + the item id list, which is enough to see whether
a family is covered at all.

Usage: python tools/report_items_without_recipes.py [--all]

`--all` prints every missing id instead of the first six of each family.
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os
import re
import sys
from collections import Counter

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RESOURCES = os.path.join(ROOT, str(native_path("src")), "main", "resources")
ITEM_SOURCE = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "registry",
                           "GTTechnological.java")

# families that are deliberately craftless (see docs/PORTING_REMAINING_2026-09-14.md)
# NOTE: the selector circuits are NOT craftless - GT6 crafts them (ItemIntegratedCircuit:58-85)
# and so does the port (recipes/hand_components/integrated_circuit_*.json).
BY_DESIGN_PREFIX = ()
BY_DESIGN_TIERS = ("_puv2", "_puv3", "_puv4", "_puv5", "_xv")

ID_LIST = re.compile(r'"([a-z0-9_]+)"')


def technological_ids() -> list[str]:
    text = open(ITEM_SOURCE, encoding="utf-8").read()
    block = text.split("static final String[] IDS = {", 1)[1].split("};", 1)[0]
    ids = ID_LIST.findall(block)
    return ids + [f"integrated_circuit_{i}" for i in range(25)]


def results() -> tuple[set[str], Counter]:
    produced: set[str] = set()
    kinds: Counter = Counter()
    for base, _, files in os.walk(os.path.join(RESOURCES, "data")):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(base, name)
            try:
                data = json.load(open(path, encoding="utf-8"))
            except (json.JSONDecodeError, UnicodeDecodeError):
                continue
            result = data.get("result")
            if isinstance(result, dict) and "item" in result:
                produced.add(result["item"].split(":")[-1])
            elif isinstance(result, str):
                produced.add(result.split(":")[-1])
            kinds[data.get("type", "?")] += 1
    return produced, kinds


def main() -> None:
    show_all = "--all" in sys.argv
    produced, kinds = results()
    missing = []
    for item in technological_ids():
        if item in produced:
            continue
        if any(item.startswith(p) for p in BY_DESIGN_PREFIX):
            continue
        if any(item.endswith(t) for t in BY_DESIGN_TIERS):
            continue
        missing.append(item)
    print(f"datapack recipes by type: {dict(kinds.most_common(5))}")
    print(f"technological items with no datapack recipe: {len(missing)}")
    for family in ("compact_", "circuit", "crystal_", "usb", "laser_", "cover", "logistics",
                   "foodmold", "slicer", "cell"):
        hits = [m for m in missing if family in m]
        if hits:
            shown = hits if show_all else hits[:6]
            print(f"  {family:12} {len(hits):3}  {', '.join(shown)}{'' if show_all or len(hits) <= 6 else ' …'}")
    print("NOTE: machine-only rows (RecipeMaps) and the runtime hand recipes "
          "(Loader_HandToolCraftingRecipes) are not visible here - the in-game test counts them.")
    report = os.path.join(ROOT, "docs", "items-without-recipes.json")
    if os.path.exists(report):
        try:
            with open(report, encoding="utf-8") as handle:
                rungame = json.load(handle)
            print(f"in-game report (items-without-recipes.json): {rungame.get('checked')} items checked, "
                  f"{rungame.get('withoutRecipe')} without any recipe, "
                  f"{rungame.get('machineRows')} machine rows")
        except (json.JSONDecodeError, UnicodeDecodeError) as error:
            print(f"cannot read items-without-recipes.json: {error}")


if __name__ == "__main__":
    main()
