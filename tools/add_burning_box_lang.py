#!/usr/bin/env python3
"""Add liquid/gas/fluidized-bed burning box lang entries to en_us.json and zh_cn.json."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json, os

BASE = str(native_path("src/main/resources/assets/gt6ce/lang"))

# Material display names (same as solid variants)
MATERIAL_NAMES = {
    "lead": "Lead",
    "bismuth": "Bismuth",
    "bronze": "Bronze",
    "arsenic_copper": "Arsenic Copper",
    "arsenic_bronze": "Arsenic Bronze",
    "invar": "Invar",
    "steel": "Steel",
    "chromium": "Chromium",
    "titanium": "Titanium",
    "netherite": "Netherite",
    "tungsten": "Tungsten",
    "tungsten_steel": "Tungstensteel",
    "ta4hfc5": "Tantalum Hafnium Carbide",
}

MATERIAL_NAMES_ZH = {
    "lead": "铅",
    "bismuth": "铋",
    "bronze": "青铜",
    "arsenic_copper": "砷铜",
    "arsenic_bronze": "砷青铜",
    "invar": "殷钢",
    "steel": "钢",
    "chromium": "铬",
    "titanium": "钛",
    "netherite": "下界合金",
    "tungsten": "钨",
    "tungsten_steel": "钨钢",
    "ta4hfc5": "碳化钽铪",
}

FUEL_TYPE_LABEL = {
    "liquid": ("Liquid", "液体"),
    "gas": ("Gas", "气体"),
    "fluidbed": ("Fluidized Bed", "流化床"),
}


def generate_entries():
    entries_en = {}
    entries_zh = {}
    for fuel_key, (fuel_en, fuel_zh) in FUEL_TYPE_LABEL.items():
        for mat_key, mat_en in MATERIAL_NAMES.items():
            block_id = f"burning_box_{fuel_key}_{mat_key}"
            entries_en[f"block.gt6ce.{block_id}"] = f"Burning Box ({fuel_en}, {mat_en})"
            entries_zh[f"block.gt6ce.{block_id}"] = f"燃烧室 ({fuel_zh}, {MATERIAL_NAMES_ZH[mat_key]})"
        for mat_key, mat_en in MATERIAL_NAMES.items():
            block_id = f"burning_box_{fuel_key}_dense_{mat_key}"
            entries_en[f"block.gt6ce.{block_id}"] = f"Dense Burning Box ({fuel_en}, {mat_en})"
            entries_zh[f"block.gt6ce.{block_id}"] = f"致密燃烧室 ({fuel_zh}, {MATERIAL_NAMES_ZH[mat_key]})"
    return entries_en, entries_zh


def update_lang_file(path, entries):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    added = 0
    for key, value in entries.items():
        if key not in data:
            data[key] = value
            added += 1
    # Sort keys for consistency
    sorted_data = {}
    for k in sorted(data.keys()):
        sorted_data[k] = data[k]
    with open(path, "w", encoding="utf-8") as f:
        json.dump(sorted_data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    return added


def main():
    entries_en, entries_zh = generate_entries()
    print(f"Generated {len(entries_en)} lang entries")

    en_path = os.path.join(BASE, "en_us.json")
    zh_path = os.path.join(BASE, "zh_cn.json")

    added_en = update_lang_file(en_path, entries_en)
    print(f"Added {added_en} entries to en_us.json")

    added_zh = update_lang_file(zh_path, entries_zh)
    print(f"Added {added_zh} entries to zh_cn.json")


if __name__ == "__main__":
    main()
