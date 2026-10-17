#!/usr/bin/env python3
"""Update boiler blockstate JSONs to include HORIZONTAL_FACING variants."""
import json, os

BASE = "platform-shared/src/main/resources/assets/gregtech"
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
