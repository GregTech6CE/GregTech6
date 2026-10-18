"""Which GT6 prefixes the port only *names* (registry entry without an item or block delegate)?

``MaterialPrefixes`` registers ~450 names; an entry is only functional when it resolves a delegate —
an item prefix from ``MaterialPrefix`` (``PrefixRegistry``) or a block prefix from a ``block("…")``
builder. The rest are placeholders: recipes referencing them resolve to nothing (and land in
``docs/missing-content-tokens.txt``).

Usage:  python tools/report_prefixless_delegates.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re

PREFIX = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java"))
REGISTRY = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefixes.java"))
BLOCKS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/api/prefix/BlockMaterialPrefix.java"))


def main() -> None:
    registered = set(re.findall(r'\bregister\(\s*"(\w+)"', REGISTRY.read_text(encoding="utf-8")))
    item_fields = set(re.findall(r"MaterialPrefix\s+(\w+)\s*;", PREFIX.read_text(encoding="utf-8")))
    block_fields = set(re.findall(r"BlockMaterialPrefix\s+(\w+)\s*;", BLOCKS.read_text(encoding="utf-8")))
    block_builders = set(re.findall(r'\bblock\(\s*"(\w+)"', REGISTRY.read_text(encoding="utf-8")))
    block_items = {re.search(r'BlockMaterialPrefix\.(\w+)\s*=', line.strip()).group(1)
                   for line in REGISTRY.read_text(encoding="utf-8").splitlines()
                   if re.match(r"\s*BlockMaterialPrefix\.(\w+)\s*=", line)}

    functional = item_fields | block_builders | block_fields
    name_only = sorted(registered - functional)
    print(f"registered names: {len(registered)}")
    print(f"  item prefixes (MaterialPrefix fields): {len(item_fields)}")
    print(f"  block prefixes (block(...) builders + BlockMaterialPrefix fields): "
          f"{len(block_builders | block_fields)}")
    print(f"  name-only entries (no delegate): {len(name_only)}")
    print("  " + ", ".join(name_only))


if __name__ == "__main__":
    main()
