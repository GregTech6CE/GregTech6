#!/usr/bin/env python3
"""Assets for the GT6 single-block Steam Boiler Tanks (F1).

All 26 variants (13 materials x {normal, strong}) share one composite block
model (colored + overlay layers, material tint via the block/item colour
handler); each id gets a blockstate + item model pointing at it, plus lang.
Textures already live in the port (machines/tanks/boiler_steam)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gregtech")
MODELS = os.path.join(ASSETS, "models", "block", "machine", "boiler")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")

# (id suffix, en, zh) — must match registry/GTBoilers.java
MATS = [
    ("lead",           "Lead",           "铅"),
    ("bismuth",        "Bismuth",        "铋"),
    ("bronze",         "Bronze",         "青铜"),
    ("arsenic_copper", "Arsenic Copper", "砷铜"),
    ("arsenic_bronze", "Arsenic Bronze", "砷青铜"),
    ("invar",          "Invar",          "殷钢"),
    ("steel",          "Steel",          "钢"),
    ("chromium",       "Chromium",       "铬"),
    ("titanium",       "Titanium",       "钛"),
    ("netherite",      "Netherite",      "下界合金"),
    ("tungsten",       "Tungsten",       "钨"),
    ("tungsten_steel", "Tungsten Steel", "钨钢"),
    ("ultimet",        "Ultimet",        "高温合金"),
]

DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}

FOLDER = "tanks/boiler_steam"
MODEL_REF = "gregtech:block/machine/boiler/steam_boiler"
FACE_TEX = {"up": "top", "down": "bottom", "north": "side", "south": "side", "west": "side", "east": "side"}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tex(sub, name):
    return f"gregtech:block/machines/{FOLDER}/{sub}/{name}"


def layer(sub, tinted):
    faces = {}
    for face, name in FACE_TEX.items():
        d = {"texture": "#" + face, "cullface": face}
        if tinted:
            d["tintindex"] = 0
        faces[face] = d
    return {
        "parent": "minecraft:block/block",
        "textures": {face: tex(sub, name) for face, name in FACE_TEX.items()},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
        "render_type": "minecraft:solid" if tinted else "minecraft:cutout",
    }


def main():
    model = {
        "loader": "forge:composite",
        "display": DISPLAY,
        "children": {"layer0": layer("colored", True), "layer1": layer("overlay", False)},
    }
    write(os.path.join(MODELS, "steam_boiler.json"), model)
    # Item models have no block entity renderer: include the source's zero-pressure gauge here.
    inventory = json.loads(json.dumps(model))
    for name, depth, texture in (("gauge", -0.08, "base"), ("needle", -0.096, "00")):
        inventory["children"][name] = {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"gauge": "gregtech:block/machines/barometer/" + texture},
            "elements": [{"from": [1, 1, depth], "to": [15, 15, depth],
                          "faces": {"north": {"texture": "#gauge", "uv": [0, 0, 16, 16]}}}],
        }
    write(os.path.join(MODELS, "steam_boiler_inventory.json"), inventory)

    lang_en, lang_zh = {}, {}
    count = 0
    for prefix, en_name, zh_name in (("steam_boiler_", "Steam Boiler Tank", "蒸汽锅炉"),
                                     ("strong_steam_boiler_", "Strong Steam Boiler Tank", "强力蒸汽锅炉")):
        for suffix, en, zh in MATS:
            device_id = prefix + suffix
            write(os.path.join(BLOCKSTATES, device_id + ".json"), {"variants": {"": {"model": MODEL_REF}}})
            write(os.path.join(ITEM_MODELS, device_id + ".json"), {"parent": MODEL_REF + "_inventory"})
            lang_en[f"block.gregtech.{device_id}"] = f"{en_name} ({en})"
            lang_zh[f"block.gregtech.{device_id}"] = f"{zh_name}({zh})"
            count += 1

    # fill the zh energy-input tooltip key (missing; also used by energy nodes)
    lang_zh["tooltip.gregtech.machine.energy_in"] = "能量输入: "

    for lang_path, entries in ((os.path.join(ASSETS, "lang", "en_us.json"), lang_en),
                               (os.path.join(ASSETS, "lang", "zh_cn.json"), lang_zh)):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        lang.update(entries)
        with open(lang_path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")
    print(f"boilers: {count}, lang entries: {len(lang_en)}")


if __name__ == "__main__":
    main()
