"""Which GT6 prefixes that the port lacks are actually referenced by GT6 recipes?

``tools/report_missing_item_prefixes.py`` lists the 93 GT6 prefixes without a port item/block form.
Not all of them are used by the original's recipes, so this counts, per GT6 loader file, how often each
missing prefix appears as a recipe ingredient/product — the ranking for which family is worth porting.

Usage:  python tools/report_missing_prefix_usage.py [--limit N]
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
import sys

GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
GT6_LOADERS = GT6 / "gregtech/loaders"
SKIP = {"blockGem", "blockIngot", "blockPlate", "blockRaw", "blockSolid", "blockPlateGem"}


def missing_prefixes() -> list[str]:
    """Names from the comparison report that have no port item/block form."""
    import json
    report = json.loads(pathlib.Path("docs/prefix-condition-comparison.json").read_text(encoding="utf-8"))
    prefix = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java")).read_text(encoding="utf-8")
    registry = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefixes.java")).read_text(encoding="utf-8")
    item_fields = set(re.findall(r"MaterialPrefix\s+(\w+)\s*;", prefix))
    blocks = set(re.findall(r'\bblock\(\s*"(\w+)"', registry))
    return [name for name in report["conditions"] if name not in item_fields and name not in blocks]


def main() -> None:
    limit = 30
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])

    names = missing_prefixes()
    usage: collections.Counter[str] = collections.Counter()
    where: dict[str, collections.Counter[str]] = {}
    files = list(GT6_LOADERS.rglob("*.java")) + list((GT6 / "gregtech/items").rglob("*.java"))
    for path in files:
        text = path.read_text(encoding="utf-8", errors="replace")
        for name in names:
            count = len(re.findall(r"\bOP\." + re.escape(name) + r"\b", text))
            if count:
                usage[name] += count
                where.setdefault(name, collections.Counter())[path.name] += count

    print(f"GT6 prefixes with no port form: {len(names)}")
    print(f"referenced by GT6 loaders/items: {len(usage)}")
    for name, count in usage.most_common(limit):
        top = ", ".join(f"{file}({n})" for file, n in where[name].most_common(3))
        print(f"  {name:22} {count:4}  {top}")
    unused = [name for name in names if name not in usage]
    print(f"never referenced ({len(unused)}): {', '.join(sorted(unused))}")


if __name__ == "__main__":
    main()
