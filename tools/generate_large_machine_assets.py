#!/usr/bin/env python3
"""Generate assets for the 13 GT6 large batch machines (textures already copied).

Per machine: block models (base/_running/_active composite colored+overlay),
per-id blockstate (facing x lit x running) and item model, plus lang entries.
Mirrors the structure of the existing 66 basic machine assets.
"""

import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregtech")
TEX = os.path.join(ASSETS, "textures", "block", "machines", "basicmachines")
SHARED = os.path.join(ROOT, "platform-shared", "src", "main", "resources", "assets", "gregtech")
MODELS = os.path.join(SHARED, "models", "block", "machine", "basic")
BLOCKSTATES = os.path.join(SHARED, "blockstates")
ITEM_MODELS = os.path.join(SHARED, "models", "item")

# name -> (material id suffix, en display, zh display)
MACHINES = {
    "largecentrifuge":   ("tungsten_steel",  "Large Centrifuge",         "大型离心机"),
    "largeelectrolyzer": ("stainless_steel", "Large Electrolyzer",       "大型电解机"),
    "largecoagulator":   ("stainless_steel", "Large Coagulator Array",   "大型凝固机阵列"),
    "largeautoclave":    ("stainless_steel", "Large Autoclave",          "大型高压釜"),
    "largebath":         ("stainless_steel", "Large Bathing Vat",        "大型浸洗池"),
    "largemixer":        ("stainless_steel", "Large Batch Mixer",        "大型批量搅拌机"),
    "largefermenter":    ("stainless_steel", "Large Fermenter",          "大型发酵罐"),
    "largeoven":         ("invar",           "Large Electric Oven",      "大型电炉"),
    "largesluice":       ("titanium",        "Large Sluice",             "大型洗矿槽"),
    "largecrusher":      ("tungsten_steel",  "Large Crusher",            "大型粉碎机"),
    "largeshredder":     ("tungsten_steel",  "Large Shredder",           "大型碎解机"),
    "largesqueezer":     ("steel",           "Large Squeezer",           "大型压榨机"),
    "largemassfab":      ("lead",            "Large Matter Fabricator",  "大型物质制造机"),
}

MAT_EN = {
    "tungsten_steel": "Tungsten Steel", "stainless_steel": "Stainless Steel",
    "invar": "Invar", "titanium": "Titanium", "steel": "Steel", "lead": "Lead",
}
MAT_ZH = {
    "tungsten_steel": "钨钢", "stainless_steel": "不锈钢",
    "invar": "殷钢", "titanium": "钛", "steel": "钢", "lead": "铅",
}

# model face -> texture file basename (GT6 west=right / east=left mirroring)
FACES = {"down": "bottom", "up": "top", "north": "front",
         "south": "back", "west": "right", "east": "left"}

DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def write(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def layer(name, folder, tinted):
    """One composite child covering the full cube with the 6 face textures."""
    textures = {}
    faces = {}
    for face, tex_name in FACES.items():
        png = os.path.join(TEX, name, folder, tex_name + ".png")
        if not os.path.exists(png):
            continue
        textures[face] = f"gregtech:block/machines/basicmachines/{name}/{folder}/{tex_name}"
        face_def = {"texture": "#" + face, "cullface": face}
        if tinted:
            face_def["tintindex"] = 0
        faces[face] = face_def
    if not faces:
        return None
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
        "render_type": "minecraft:solid" if tinted else "minecraft:cutout",
    }


def build_model(name, overlay_folder):
    layer0 = layer(name, "colored", True)
    layer1 = layer(name, overlay_folder, False)
    if layer0 is None:
        return None
    children = {"layer0": layer0}
    if layer1 is not None:
        children["layer1"] = layer1
    return {"loader": "forge:composite", "display": DISPLAY, "children": children}


def blockstate(name, has_running, has_active):
    base = f"gregtech:block/machine/basic/{name}"
    running = base + "_running" if has_running else base
    active = base + "_active" if has_active else running
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            for run in ("false", "true"):
                model = active if lit == "true" else (running if run == "true" else base)
                entry = {"model": model}
                if rot:
                    entry["y"] = rot
                variants[f"facing={facing},lit={lit},running={run}"] = entry
    return {"variants": variants}


def main():
    models = blockstates = items = 0
    lang_en, lang_zh = {}, {}
    for name, (mat, en, zh) in MACHINES.items():
        base_model = build_model(name, "overlay")
        if base_model is None:
            print(f"!! {name}: no colored textures, skipped")
            continue
        write(os.path.join(MODELS, name + ".json"), base_model)
        models += 1
        has = {}
        for suffix, folder in (("_running", "overlay_running"), ("_active", "overlay_active")):
            m = build_model(name, folder)
            has[suffix] = m is not None and "layer1" in m["children"]
            if has[suffix]:
                write(os.path.join(MODELS, name + suffix + ".json"), m)
                models += 1
        machine_id = f"{name}_{mat}"
        write(os.path.join(BLOCKSTATES, machine_id + ".json"),
              blockstate(name, has["_running"], has["_active"]))
        blockstates += 1
        item_parent = name + ("_active" if has["_active"] else "")
        write(os.path.join(ITEM_MODELS, machine_id + ".json"),
              {"parent": f"gregtech:block/machine/basic/{item_parent}"})
        items += 1
        lang_en[f"block.gregtech.{machine_id}"] = f"{en} ({MAT_EN[mat]})"
        lang_zh[f"block.gregtech.{machine_id}"] = f"{zh}({MAT_ZH[mat]})"

    for lang_path, entries in ((os.path.join(ASSETS, "lang", "en_us.json"), lang_en),
                               (os.path.join(ASSETS, "lang", "zh_cn.json"), lang_zh)):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        lang.update(entries)
        with open(lang_path, "w", encoding="utf-8") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False, sort_keys=True)
            f.write("\n")

    print(f"models: {models}, blockstates: {blockstates}, item models: {items}, lang: {len(lang_en)}")


if __name__ == "__main__":
    main()
