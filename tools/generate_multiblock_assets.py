#!/usr/bin/env python3
"""Assets for the multiblock part blocks and the Distillation Tower controller."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import os
import shutil

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\resources\assets\gregtech\textures\blocks\machines\multiblockparts"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, str(native_path("src")), "main", "resources", "assets", "gt6ce")
TEX = os.path.join(ASSETS, "textures", "block", "machines", "multiblockparts")
MODELS = os.path.join(ASSETS, "models", "block", "machine", "multiblock")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
ITEM_MODELS = os.path.join(ASSETS, "models", "item")

# (id, gt6 folder, en, zh)
PARTS = [
    ("heat_transmitter", "heatacceptor/0", "Heat Transmitter", "热传输器"),
    ("distillation_tower_part", "distillationtowerparts/0", "Distillation Tower Part", "蒸馏塔部件"),
]


DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}

def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def layer(folder, sub, tinted):
    tex = lambda n: f"gt6ce:block/machines/multiblockparts/{folder}/{sub}/{n}"
    faces = {}
    textures = {}
    for face, name in (("up", "top"), ("down", "bottom"), ("north", "side"),
                       ("south", "side"), ("west", "side"), ("east", "side")):
        textures[face] = tex(name)
        d = {"texture": "#" + face, "cullface": face}
        if tinted:
            d["tintindex"] = 0
        faces[face] = d
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
        "render_type": "minecraft:solid" if tinted else "minecraft:cutout",
    }


def main():
    copied = 0
    for _, folder, _, _ in PARTS:
        src = os.path.join(GT6, folder.replace("/", os.sep))
        dst = os.path.join(TEX, folder.replace("/", os.sep))
        for base, _, files in os.walk(src):
            for f in files:
                s = os.path.join(base, f)
                d = os.path.join(dst, os.path.relpath(s, src))
                os.makedirs(os.path.dirname(d), exist_ok=True)
                shutil.copy(s, d)
                copied += 1
    print(f"textures copied: {copied}")

    lang_en, lang_zh = {}, {}
    for part_id, folder, en, zh in PARTS:
        model = {"loader": "forge:composite", "display": DISPLAY,
                 "children": {"layer0": layer(folder, "colored", True),
                              "layer1": layer(folder, "overlay", False)}}
        write(os.path.join(MODELS, part_id + ".json"), model)
        ref = f"gt6ce:block/machine/multiblock/{part_id}"
        write(os.path.join(BLOCKSTATES, part_id + ".json"), {"variants": {"": {"model": ref}}})
        write(os.path.join(ITEM_MODELS, part_id + ".json"), {"parent": ref})
        lang_en[f"block.gt6ce.{part_id}"] = en
        lang_zh[f"block.gt6ce.{part_id}"] = zh

    # controller: reuse the basic machine distillationtower models
    cid = "distillation_tower_main"
    variants = {}
    base = "gt6ce:block/machine/basic/distillationtower"
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            for run in ("false", "true"):
                model = base + ("_active" if lit == "true" else ("_running" if run == "true" else ""))
                entry = {"model": model}
                if rot:
                    entry["y"] = rot
                variants[f"facing={facing},lit={lit},running={run}"] = entry
    write(os.path.join(BLOCKSTATES, cid + ".json"), {"variants": variants})
    write(os.path.join(ITEM_MODELS, cid + ".json"), {"parent": base + "_active"})
    lang_en[f"block.gt6ce.{cid}"] = "Distillation Tower (Multiblock Main)"
    lang_zh[f"block.gt6ce.{cid}"] = "蒸馏塔(多方块主机)"
    lang_en["gt.tooltip.multiblock.distillationtower.1"] = "3x3 Base of Heat Transmitters"
    lang_zh["gt.tooltip.multiblock.distillationtower.1"] = "底部 3x3 热传输器"
    lang_en["gt.tooltip.multiblock.distillationtower.2"] = "3x3x8 of Distillation Tower Parts"
    lang_zh["gt.tooltip.multiblock.distillationtower.2"] = "3x3x8 蒸馏塔部件"
    lang_en["gt.tooltip.multiblock.distillationtower.3"] = "Main centered on side-bottom of tower facing outwards"
    lang_zh["gt.tooltip.multiblock.distillationtower.3"] = "主机位于塔底层正面中央朝外"

    for lang_path, entries in ((os.path.join(ASSETS, "lang", "en_us.json"), lang_en),
                               (os.path.join(ASSETS, "lang", "zh_cn.json"), lang_zh)):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        lang.update(entries)
        with open(lang_path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")
    print(f"parts: {len(PARTS)}, lang: {len(lang_en)}")


if __name__ == "__main__":
    main()
