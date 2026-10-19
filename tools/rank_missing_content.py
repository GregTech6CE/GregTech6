#!/usr/bin/env python3
"""Rank the transpiler's missing-content tokens by how many recipes each one blocks.

The runtime records which content tokens failed to resolve (see GeneratedRecipeResolver.missingContent),
but not how often. Counting the specs inside the generated tables shows which missing material
form, fluid or item is worth adding first.

Usage: python tools/rank_missing_content.py <missing-tokens-file>
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import collections
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
GEN_DIR = os.path.join(ROOT, "core", "src", "main", "java", "com", "gregtech", "gregtech", "content", "recipe", "generated")
SPEC = re.compile(r'"([a-z]+:[^"]+)"')


def spec_counts():
    counts = collections.Counter()
    for name in sorted(os.listdir(GEN_DIR)):
        if not name.startswith("GT") or not name.endswith("Gen.java"):
            continue
        with open(os.path.join(GEN_DIR, name), encoding="utf-8") as handle:
            for spec in SPEC.findall(handle.read()):
                if ":" in spec:
                    counts[spec] += 1
    return counts


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    with open(sys.argv[1], encoding="utf-8") as handle:
        missing = [line.strip() for line in handle if line.strip()]
    counts = spec_counts()
    rows = []
    for token in missing:
        exact = counts.get(token, 0)
        by_prefix = sum(v for k, v in counts.items() if k.startswith(token + ":"))
        rows.append((exact + by_prefix, token))
    rows.sort(key=lambda row: (-row[0], row[1]))
    print("distinct missing tokens: %d, recipe occurrences blocked: %d"
          % (len(rows), sum(r[0] for r in rows)))
    print("\n== biggest blockers ==")
    for count, token in rows[:30]:
        print("  %-5d %s" % (count, token))
    print("\n== blocked recipes per kind ==")
    kinds = collections.Counter()
    for count, token in rows:
        kinds[token.split(":")[0]] += count
    for kind, count in kinds.most_common():
        print("  %-6s %d" % (kind, count))
    print("\n== most common material forms still missing ==")
    forms = collections.Counter()
    for count, token in rows:
        parts = token.split(":")
        if parts[0] == "i" and len(parts) == 4:
            forms[parts[1]] += count
    for form, count in forms.most_common(20):
        print("  %-22s %d" % (form, count))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
