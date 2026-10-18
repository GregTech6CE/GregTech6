#!/usr/bin/env python3
"""Which GT6 files carry recipes, and does a port loader claim to cover them?

The transpiler only reads `gregtech/loaders/c/Loader_Recipes_*.java`; GT6 also registers hundreds of
recipes inside its *item* and *block* classes (`MultiItemFood`, `MultiItemTechnological`,
`MultiItemRandomTools`, …). The port covers many of those with hand-written loaders that name the
GT6 file they came from, so this pairs every GT6 recipe-carrying file with the port files that
mention it and reports the ones nothing claims.

Inputs : gregtech6-master sources, the port's src/main/java
Output : stdout report (row counts per GT6 file, and the unclaimed ones)
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import os
import re

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java"
PORT = str(native_path("src/main/java"))

# GT6 recipe registration calls worth counting.
PATTERNS = [r"\bRM\.\w+\.add(?:Fake)?Recipe", r"\bCR\.(?:shaped|shapeless)",
            r"\bRM\.replicateOrganic", r"\.food_can\(", r"\bRecipeMap\.\w+\.add(?:Fake)?Recipe"]

# Files that are not recipe sources but mention the calls in comments/helpers.
SKIP = {"Recipe.java", "RecipeMap.java", "CR.java", "RM.java", "Loader_Recipes_Foreign.java"}


def port_text() -> dict:
    out = {}
    for root, _dirs, files in os.walk(PORT):
        for f in files:
            if f.endswith(".java"):
                p = os.path.join(root, f)
                out[p.replace("\\", "/")] = open(p, encoding="utf-8", errors="replace").read()
    return out


def main() -> int:
    port = port_text()
    rows = []
    for root, _dirs, files in os.walk(GT6):
        for f in files:
            if not f.endswith(".java") or f in SKIP:
                continue
            path = os.path.join(root, f)
            text = open(path, encoding="utf-8", errors="replace").read()
            count = sum(len(re.findall(p, text)) for p in PATTERNS)
            if count == 0:
                continue
            rel = os.path.relpath(path, GT6).replace("\\", "/")
            # Port loaders cite their GT6 source either as `Foo.java` or as `{@code Foo}`.
            stem = f[:-5]
            claims = [p for p, t in port.items() if f in t or stem in t]
            rows.append((count, rel, claims))
    rows.sort(reverse=True)
    unclaimed = [r for r in rows if not r[2]]
    print("%-6s %-52s %s" % ("rows", "GT6 file", "port files naming it"))
    for count, rel, claims in rows[:30]:
        print("%-6d %-52s %s" % (count, rel, ", ".join(c.split("gregtech/")[-1] for c in claims[:2])
                                 if claims else "*** NOTHING CLAIMS IT ***"))
    print()
    print("recipe-carrying GT6 files: %d, unclaimed: %d (= %d rows)"
          % (len(rows), len(unclaimed), sum(r[0] for r in unclaimed)))
    for count, rel, _claims in unclaimed:
        print("   %-6d %s" % (count, rel))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
