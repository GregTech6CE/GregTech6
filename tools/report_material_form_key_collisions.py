"""Check key collisions in the material form tables.

Two places can collide:

  * the **generated table** (``MaterialForms.DATA``) — a normalised key must appear exactly once, or a
    HashMap lookup would depend on row order (this must be 0);
  * the **raw report** (``docs/gt6-form-flags.json``) — GT6 declares names that normalise alike
    (``Co`` cobalt / ``CO`` carbon monoxide, ``Gold`` the wood / the ``gold()`` factory in
    ``MT.java:468``); those names are left out of the generated table on purpose and are listed here
    for review, with the ids that still cover them.

Usage:  python tools/report_material_form_key_collisions.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import collections
import json
import pathlib
import re

REPORT = pathlib.Path("docs/gt6-form-flags.json")
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
ROW = re.compile(r'^\s+"([^"|]+)\|([A-Z_,]*)"', re.M)


def normalize(name: str) -> str:
    return re.sub(r"[^a-z0-9]", "", name.lower())


def main() -> None:
    rows = ROW.findall(FORMS.read_text(encoding="utf-8"))
    duplicates = [key for key, count in collections.Counter(k for k, _ in rows).items() if count > 1]
    ids = {key: flags for key, flags in rows if key.isdigit()}
    names = {key for key, _ in rows if not key.isdigit()}
    print(f"generated table: {len(rows)} rows ({len(ids)} id keys, {len(names)} name keys), "
          f"duplicate keys: {len(duplicates)}")
    for key in duplicates:
        print(f"  DUPLICATE {key}")

    materials = json.loads(REPORT.read_text(encoding="utf-8"))["materials"]
    grouped: dict[str, list[str]] = collections.defaultdict(list)
    for key in materials:
        grouped[normalize(key)].append(key)
    collisions = {k: v for k, v in grouped.items() if len(v) > 1}
    print(f"raw report: {len(materials)} declarations, normalising alike: {len(collisions)}")
    for key, group in sorted(collisions.items()):
        covered = [name for name in group
                   if any(str(i) in ids for i in materials[name].get("ids", []))]
        state = "covered by id" if len(covered) == len(group) else "PARTLY UNCOVERED"
        print(f"  {key}: {', '.join(sorted(group))}  ({state})")


if __name__ == "__main__":
    main()
