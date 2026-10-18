#!/usr/bin/env python3
"""Update boiler blockstate JSONs to include HORIZONTAL_FACING variants."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json, os

BASE = str(native_path("src/main/resources/assets/gt6ce"))
BS_DIR = os.path.join(BASE, "blockstates")

updated = 0
for name in os.listdir(BS_DIR):
    if "steam_boiler" not in name and "strong_steam_boiler" not in name:
        continue
    path = os.path.join(BS_DIR, name)
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)
    if "" not in data.get("variants", {}):
        continue
    model = data["variants"][""]["model"]
    data["variants"] = {}
    # Always register all four facing keys so the blockstate property has full coverage.
    data["variants"]["facing=north"] = {"model": model}
    data["variants"]["facing=south"] = {"model": model, "y": 180}
    data["variants"]["facing=west"] = {"model": model, "y": 270}
    data["variants"]["facing=east"] = {"model": model, "y": 90}
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
    updated += 1
    print(f"  {name}")

print(f"\nUpdated {updated} boiler blockstates.")
