"""Look up port MultiItem ids by GT6 display name (used while mapping GT6 loot rows).

Usage:  python tools/find_multiitem_id.py Milk Honey Blood Tar Glue Mercury Indigo Beer Spoiled Empty
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re
import sys

GEN = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/registry/GTMultiItemsGen.java"))


def main() -> None:
    rows = re.findall(r'"([a-z0-9_]+)", "((?:\\.|[^"\\])*)", "(\w+)", ""', GEN.read_text(encoding="utf-8"))
    queries = [q.lower() for q in sys.argv[1:]]
    for query in queries:
        hits = [(i, d, c) for i, d, c in rows if query in d.lower() or query in i]
        print(f"--- {query!r}: {len(hits)} hit(s)")
        for i, d, c in hits[:12]:
            print(f"    {c:12} {i:28} {d}")


if __name__ == "__main__":
    main()
