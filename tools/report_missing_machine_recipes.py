#!/usr/bin/env python3
"""Report which registered single-block machines have no crafting recipe.

The machine crafting recipes live in code (`data/BasicMachineCraftingRecipes.java`, registered as
data-pack recipes when the server starts), not as JSON under `data/gt6ce/recipes/`, so this
compares the generated tables instead of scanning for recipe files:

* `data/BasicMachineOriginalParams.java` — every registered machine variant (`machine`, `tier`),
* `data/BasicMachineCraftingRecipes.java` — every crafting row GT6 has for one of them,
* `data/MultiblockCraftingRecipes.java` — the multiblock controllers, which have their own table.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = native_path(ROOT / "src/main/java/com/gregtech/gregtech")

PARAMS = JAVA / "data/BasicMachineOriginalParams.java"
CRAFTING = JAVA / "data/BasicMachineCraftingRecipes.java"
MULTIBLOCK = JAVA / "data/MultiblockCraftingRecipes.java"
PACK = JAVA / "data/BasicMachineRecipePack.java"


def multiblock_controllers() -> set:
    """{@code BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS}: crafting lives in the multiblock table."""
    body = re.search(r"MULTIBLOCK_CONTROLLERS\s*=\s*Set\.of\((.*?)\);",
                     PACK.read_text(encoding="utf-8"), re.S)
    return set(re.findall(r'"([a-z_0-9]+)"', body.group(1))) if body else set()


def variants() -> set:
    rows = re.findall(r'new Params\("([a-z_0-9]+)",\s*(\d+),', PARAMS.read_text(encoding="utf-8"))
    return {(name, int(tier)) for name, tier in rows}


def crafting_rows() -> set:
    rows = re.findall(r'new Entry\("([a-z_0-9]+)",\s*(\d+),', CRAFTING.read_text(encoding="utf-8"))
    return {(name, int(tier)) for name, tier in rows}


def multiblock_rows() -> list:
    return re.findall(r'new Entry\("([a-z_0-9]+)"', MULTIBLOCK.read_text(encoding="utf-8"))


registered = variants()
crafted = crafting_rows()
controllers = multiblock_controllers()
missing = sorted(row for row in registered - crafted if row[0] not in controllers)
covered_elsewhere = sorted(row for row in registered - crafted if row[0] in controllers)
extra = sorted(crafted - registered)

print("registered machine variants: %d" % len(registered))
print("crafting rows (in code):     %d" % len(crafted))
print("multiblock controller rows:  %d (%d controller variants covered there)"
      % (len(multiblock_rows()), len(covered_elsewhere)))
print()
if missing:
    print("registered variants without a crafting row: %d" % len(missing))
    for name, tier in missing[:40]:
        print("    %s t%d" % (name, tier))
else:
    print("every registered variant has a crafting row")
if extra:
    print()
    print("crafting rows without a registered variant (tables out of step?): %d" % len(extra))
    for name, tier in extra[:40]:
        print("    %s t%d" % (name, tier))
