"""Give the 320 "scanned" bumblebee items the same item models as their living siblings.

GT6 keeps one bee item per species+type and shows the scan state through the item's meta; the port
registers one item per (species, type), so the four scanned types added in §77 need their own
`assets/gregtech/models/item/<id>.json`. GT6 has no separate scanned artwork in the resource pack
(only `overlay_princess/queen/dead`), so a scanned bee reuses its living sibling's model - layer0 is
the species texture and layer1 the type overlay.

Idempotent: files whose content already matches are left alone.

Usage: python tools/generate_bumble_scanned_models.py [--check]
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import argparse
import io
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech", "models", "item")
SPECIES = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "bumble",
                       "GTBumbleSpecies.java")

TYPES = ("drone", "princess", "queen", "dead")


def sources():
    text = io.open(SPECIES, encoding="utf-8").read()
    out = []
    for line in text.split("\n"):
        line = line.strip()
        if not line.startswith("new Species("):
            continue
        parts = [p.strip().strip('"') for p in line[len("new Species("):].split("),")[0].split(",")]
        prefix = parts[1]
        out.append(prefix)
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report differences without writing")
    args = parser.parse_args()

    written, missing_source = [], []
    for prefix in sources():
        for kind in TYPES:
            living = os.path.join(ITEMS, "%s_%s.json" % (prefix, kind))
            if not os.path.exists(living):
                missing_source.append("%s_%s" % (prefix, kind))
                continue
            model = io.open(living, encoding="utf-8").read()
            target = os.path.join(ITEMS, "%s_scanned_%s.json" % (prefix, kind))
            if os.path.exists(target) and io.open(target, encoding="utf-8").read() == model:
                continue
            written.append(os.path.basename(target))
            if not args.check:
                io.open(target, "w", encoding="utf-8", newline="\n").write(model)

    print(("would write " if args.check else "wrote ") + str(len(written)) + " scanned bee models")
    if missing_source:
        print("living models missing (%d): %s" % (len(missing_source), missing_source[:8]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
