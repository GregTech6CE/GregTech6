#!/usr/bin/env python3
"""Generate item model JSONs for liquid/gas/fluidized-bed burning boxes."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json, os

ITEM_DIR = str(native_path("src/main/resources/assets/gregtech/models/item"))

MATERIALS = [
    "lead", "bismuth", "bronze", "arsenic_copper", "arsenic_bronze",
    "invar", "steel", "chromium", "titanium", "netherite",
    "tungsten", "tungsten_steel", "ta4hfc5",
]

FUEL_TYPES = {
    "liquid": "burning_liquid",
    "gas": "burning_gas",
    "fluidbed": "burning_fluidbed",
}


def main():
    count = 0
    for fuel_key, tex_set in FUEL_TYPES.items():
        for mat in MATERIALS:
            # Normal
            path = os.path.join(ITEM_DIR, f"burning_box_{fuel_key}_{mat}.json")
            model = {"parent": f"gregtech:block/machine/{tex_set}/north_off"}
            with open(path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)
                f.write("\n")
            count += 1
            # Dense
            path = os.path.join(ITEM_DIR, f"burning_box_{fuel_key}_dense_{mat}.json")
            with open(path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)
                f.write("\n")
            count += 1
    print(f"Created {count} item model files")


if __name__ == "__main__":
    main()
