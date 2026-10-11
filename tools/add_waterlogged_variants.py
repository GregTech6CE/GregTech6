"""Add waterlogged=false/true variants to blockstate JSON files that need them.

Categories:
  1. engines (42): facing + lit -> facing,lit,waterlogged
  2. chest/storage (124): facing -> facing,waterlogged
  3. energy nodes (40): facing -> facing,waterlogged
  4. stone slabs (432): facing -> facing,waterlogged
  5. faucets (39): facing -> facing,waterlogged

Skips: pipes/wires/axles/molds/crucibles/crossing/ender_garbage (all use ""
  catch-all variant), energy_storage (5, not waterloggable), large_turbine_main
  (multiblock part, not waterloggable); solar_panel_* (full cubes, not waterloggable).
"""

import json
import os
import re
from pathlib import Path

BLOCKSTATES_DIR = Path(r"F:\Dev\GregTech6\GregTech6\src\main\resources\assets\gregtech\blockstates")


def needs_waterlogged(filename: str) -> bool:
    """Check if this file needs waterlogged variants added."""
    # Engines: engine_*.json (42 files)
    if filename.startswith("engine_"):
        return True

    # Chest/storage: chest_*, locker, safe, drawer_quad, mass_storage_* (124 files)
    if (filename.startswith("chest_")
            or filename in ("locker.json", "safe.json", "drawer_quad.json")
            or filename.startswith("mass_storage_")):
        return True

    # Energy nodes (40 files)
    if (filename.startswith("electric_motor_")
            or filename.startswith("electric_dynamo_")
            or filename.startswith("flux_motor_")
            or filename.startswith("flux_dynamo_")
            or filename.startswith("steam_turbine_")
            or filename.startswith("rotation_transformer_")
            or filename.startswith("transformer_")
            or filename.startswith("battery_box_")):
        return True

    # Stone slabs: *_slab.json (432 files)
    if filename.endswith("_slab.json"):
        return True

    # Faucets: crucible_faucet_*.json (39 files)
    if filename.startswith("crucible_faucet_"):
        return True

    return False


def already_has_waterlogged(data: dict) -> bool:
    """Check if any variant key already contains 'waterlogged'."""
    if "variants" not in data:
        return True  # multipart or unknown, skip
    for key in data["variants"]:
        if "waterlogged" in key:
            return True
    return False


def process_file(filepath: Path) -> bool:
    """Add waterlogged variants to a single blockstate file. Returns True if modified."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    data = json.loads(content)

    if already_has_waterlogged(data):
        return False

    variants = data.get("variants", {})
    if not variants:
        return False

    new_variants = {}
    for key, model_data in variants.items():
        new_variants[key + ",waterlogged=false"] = model_data
        new_variants[key + ",waterlogged=true"] = model_data

    data["variants"] = new_variants

    # Write with same formatting as original (compact, 2-space indent)
    output = json.dumps(data, indent=2, ensure_ascii=False)
    # json.dumps adds trailing newline; match original style
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(output)
        f.write("\n")

    return True


def main():
    files = sorted(os.listdir(BLOCKSTATES_DIR))
    candidates = [f for f in files if f.endswith(".json") and needs_waterlogged(f)]

    print(f"Found {len(candidates)} candidate files")

    modified = 0
    skipped_already = 0
    for filename in candidates:
        filepath = BLOCKSTATES_DIR / filename
        try:
            if process_file(filepath):
                modified += 1
            else:
                skipped_already += 1
                print(f"  SKIP (already has waterlogged): {filename}")
        except Exception as e:
            print(f"  ERROR in {filename}: {e}")

    print(f"\nModified: {modified}")
    print(f"Skipped (already waterlogged): {skipped_already}")
    print(f"Total processed: {modified + skipped_already} / {len(candidates)}")


if __name__ == "__main__":
    main()
