#!/usr/bin/env python3
"""Assets for the GT6 energy-net node devices (motors/dynamos/transformers/
turbines/solar/battery boxes/storage cabinets): copies the GT6 textures and
emits composite block models, facing blockstates, item models and lang."""

import json
import os
import shutil
import argparse
import re
from pathlib import Path

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\resources\assets\gregtech\textures\blocks\machines"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "gregtech")
TEX = os.path.join(ASSETS, "textures", "block", "machines")
SHARED = os.path.join(ROOT, "platform-shared", "src", "main", "resources", "assets", "gregtech")
MODELS = os.path.join(SHARED, "models", "block", "machine", "energy")
BLOCKSTATES = os.path.join(SHARED, "blockstates")
ITEM_MODELS = os.path.join(SHARED, "models", "item")

TIERS = [("lv", 32), ("mv", 128), ("hv", 512), ("ev", 2048), ("iv", 8192)]
STEPS = ["ulv_lv", "lv_mv", "mv_hv", "hv_ev", "ev_iv", "iv_luv", "luv_zpm", "zpm_uv", "uv_xv"]
BATTERY_TIERS = ["ulv","lv","mv","hv","ev","iv","luv","zpm","uv","xv"]
BATTERY_NAMES = ["ULV","LV","MV","HV","EV","IV","LuV","ZPM","UV","PUV1"]
TURBINES = [("bronze", "青铜"), ("brass", "黄铜"), ("invar", "殷钢"), ("steel", "钢"), ("chromium", "铬")]

# (id, texture folder, face layout, rotate?, en, zh)
DEVICES = []
for name, _ in TIERS:
    DEVICES.append((f"electric_motor_{name}", "motors/rotation_electric", "fbs", True,
                    f"Electric Motor ({name.upper()})", f"电动机({name.upper()})"))
    DEVICES.append((f"electric_dynamo_{name}", "dynamos/electric_rotation", "fbs", True,
                    f"Electric Dynamo ({name.upper()})", f"发电机({name.upper()})"))
for s in STEPS:
    DEVICES.append((f"transformer_{s}", "transformers/transformer_electric", "fbs", True,
                    f"Transformer ({s.replace('_', '-').upper()})", f"变压器({s.replace('_', '-').upper()})"))
for name, zh in TURBINES:
    DEVICES.append((f"steam_turbine_{name}", "turbines/rotation_steam", "fbs", True,
                    f"Steam Turbine ({name.replace('_', ' ').title()})", f"蒸汽轮机({zh})"))
DEVICES.append(("solar_panel_silicon", "solarpanels/solarpanel_electric_8eu", "tbs", False,
                "Solar Panel (Silicon)", "太阳能板(硅)"))
DEVICES.append(("solar_panel_germanium", "solarpanels/solarpanel_electric_8eu", "tbs", False,
                "Solar Panel (Germanium)", "太阳能板(锗)"))
DEVICES.append(("flux_motor_lv", "motors/rotation_flux", "fbs", True,
                "Flux Motor (LV)", "通量电动机(LV)"))
DEVICES.append(("flux_motor_mv", "motors/rotation_flux", "fbs", True,
                "Flux Motor (MV)", "通量电动机(MV)"))
DEVICES.append(("flux_dynamo_lv", "dynamos/flux_rotation", "fbs", True,
                "Flux Dynamo (LV)", "通量发电机(LV)"))
DEVICES.append(("flux_dynamo_mv", "dynamos/flux_rotation", "fbs", True,
                "Flux Dynamo (MV)", "通量发电机(MV)"))

for tier,name in enumerate(BATTERY_TIERS):
    for large in (False,True):
        ident=("energy_storage_" if large else "battery_box_")+name
        label=("Large Battery Box" if large else "Battery Box")+" ("+BATTERY_NAMES[tier]+")"
        DEVICES.append((ident,"energystorages/battery_electric"+("_large" if large else ""),"bat",True,label,label))

def is_battery_box(ident):
    return ident.startswith(("battery_box_","energy_storage_"))

DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}

COPY_DIRS = sorted({d[1] for d in DEVICES})


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if not os.path.exists(path) or Path(path).read_text(encoding="utf-8") != text:
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)


def copy_textures(folders=None):
    copied = 0
    for rel in (COPY_DIRS if folders is None else folders):
        src = os.path.join(GT6, rel)
        dst = os.path.join(TEX, rel.replace("/", os.sep))
        for base, _, files in os.walk(src):
            for f in files:
                s = os.path.join(base, f)
                r = os.path.relpath(s, src)
                d = os.path.join(dst, r)
                os.makedirs(os.path.dirname(d), exist_ok=True)
                if not os.path.exists(d) or Path(s).read_bytes()!=Path(d).read_bytes():
                    shutil.copy(s, d)
                copied += 1
    print(f"textures copied: {copied}")


def tex_ref(folder, sub, name):
    path = f"{folder}/{sub}/{name}" if sub else f"{folder}/{name}"
    return f"gregtech:block/machines/{path}"


def faces_for(layout, folder, sub, facing="down"):
    """face -> texture name, per layout; returns None for missing files."""
    def exists(name):
        parts = [TEX, folder.replace("/", os.sep)] + ([sub] if sub else []) + [name + ".png"]
        return os.path.exists(os.path.join(*parts))
    if layout == "fbs":
        names = {"north": "front", "south": "back", "up": "side", "down": "side",
                 "west": "side", "east": "side"}
    elif layout == "tbs":
        names = {"up": "top", "down": "bottom", "north": "side", "south": "side",
                 "west": "side", "east": "side"}
        # MultiTileEntitySolarPanelElectric keeps its collector on top as its output rotates.
        if facing != "up":
            names[facing] += "_facing"
    elif layout == "bat":
        names = {"north": "front", "south": "side", "up": "side", "down": "side",
                 "west": "side", "east": "side"}
    elif layout == "cell":
        names = {"up": "top", "down": "bottom", "north": "sides", "south": "sides",
                 "west": "sides", "east": "sides"}
    else:  # fs
        names = {"north": "front", "south": "side", "up": "side", "down": "side",
                 "west": "side", "east": "side"}
    return {face: name for face, name in names.items() if exists(name)}


def layer(folder, sub, layout, tinted, device_box=None, facing="down"):
    face_names = faces_for(layout, folder, sub, facing)
    if not face_names:
        return None
    textures = {face: tex_ref(folder, sub, name) for face, name in face_names.items()}
    faces = {}
    for face in face_names:
        d = {"texture": "#" + face, "cullface": face}
        if tinted:
            d["tintindex"] = 0
        faces[face] = d
    # Solar panels inherit GT6's full-block bounds; only explicit device bounds are inset.
    if device_box is not None:
        box = device_box
    else:
        box = ([0, 0, 0], [16, 16, 16])
    if box[1] != [16, 16, 16]:
        for face in faces.values():
            face.pop("cullface", None)
        if "down" in faces:
            faces["down"]["cullface"] = "down"
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [{"from": box[0], "to": box[1], "faces": faces}],
        "render_type": "minecraft:solid" if tinted else "minecraft:cutout",
    }


def build_model(folder, layout, device_box=None, overlay="overlay", facing="down"):
    # placeable batteries: textures sit directly in the tier folder, untinted
    sub0 = "" if layout == "cell" else "colored"
    layer0 = layer(folder, sub0, layout, layout != "cell", device_box, facing)
    if layer0 is None:
        return None
    children = {"layer0": layer0}
    if layout != "cell":
        layer1 = layer(folder, overlay, layout, False, device_box, facing)
        if layer1 is not None:
            children["layer1"] = layer1
    return {"loader": "forge:composite", "display": DISPLAY, "children": children}


def blockstate(model_ref, rotate):
    variants = {}
    if rotate:
        rots = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
                "up": {"x": 270}, "down": {"x": 90}}
    else:
        rots = {f: {} for f in ("north", "east", "south", "west", "up", "down")}
    for facing, rot in rots.items():
        entry = {"model": model_ref}
        entry.update(rot)
        variants[f"facing={facing}"] = entry
    return {"variants": variants}


def main(battery_only=False, storage_transformers_only=False):
    devices = [d for d in DEVICES if is_battery_box(d[0]) or storage_transformers_only and d[0].startswith("transformer_")] if battery_only or storage_transformers_only else DEVICES
    copy_textures(sorted({d[1] for d in devices}))
    cn_path=os.path.join(ROOT,"..","GregTech.lang")
    with open(cn_path,encoding="utf-8") as f: cn=f.read()
    models = 0
    lang_en, lang_zh = {}, {}
    for device_id, folder, layout, rotate, en, zh in devices:
        model = build_model(folder, layout)
        if model is None:
            print(f"!! {device_id}: missing textures in {folder}")
            continue
        write(os.path.join(MODELS, device_id + ".json"), model)
        model_ref = f"gregtech:block/machine/energy/{device_id}"
        states=blockstate(model_ref, rotate)
        if layout == "tbs":
            for facing in ("north", "south", "west", "east"):
                write(os.path.join(MODELS, device_id + "_" + facing + ".json"),
                      build_model(folder, layout, facing=facing))
                states["variants"]["facing=" + facing] = {"model": model_ref + "_" + facing}
        if layout == "tbs":
            variants = {}
            for facing in ("down", "north", "south", "west", "east", "up"):
                suffix = "" if facing in ("down", "up") else "_" + facing
                write(os.path.join(MODELS, device_id + suffix + "_active.json"),
                      build_model(folder, layout, overlay="overlay_active", facing="down" if facing == "up" else facing))
                for active in ("false", "true"):
                    variants["facing=" + facing + ",solar_active=" + active] = {
                        "model": model_ref + suffix + ("_active" if active == "true" else "")}
            states = {"variants": variants}
        if is_battery_box(device_id) or device_id.startswith("transformer_"):
            state_property="charge_state" if is_battery_box(device_id) else "activity"
            variants={}
            for charge,overlay in enumerate(("overlay","overlay_active","overlay_blinking")):
                ref=model_ref if charge==0 else model_ref+"_"+str(charge)
                if charge: write(os.path.join(MODELS,device_id+"_"+str(charge)+".json"),build_model(folder,layout,overlay=overlay))
                for key,entry in blockstate(ref,rotate)["variants"].items():
                    for water in ("false","true"):
                        variants[key+","+state_property+"="+str(charge)+",waterlogged="+water]=entry
            states={"variants":variants}
        if is_battery_box(device_id):
            large=device_id.startswith("energy_storage_")
            tier=BATTERY_TIERS.index(device_id.removeprefix("energy_storage_" if large else "battery_box_"))
            match=re.search(r"^\s*S:gt\.multitileentity\."+str((10090 if large else 10080)+tier)+r"=(.*)$",cn,re.M)
            if not match: raise RuntimeError("Missing original battery-box translation")
            zh=match.group(1).strip()
        if device_id.startswith("transformer_"):
            number=10040+STEPS.index(device_id.removeprefix("transformer_"))
            match=re.search(r"^\s*S:gt\.multitileentity\."+str(number)+r"=(.*)$",cn,re.M)
            if not match: raise RuntimeError("Missing original transformer translation")
            zh=match.group(1).strip()
        write(os.path.join(BLOCKSTATES, device_id + ".json"), states)
        write(os.path.join(ITEM_MODELS, device_id + ".json"), {"parent": model_ref})
        lang_en[f"block.gregtech.{device_id}"] = en
        lang_zh[f"block.gregtech.{device_id}"] = zh
        models += 1

    for lang_path, entries in ((os.path.join(ASSETS, "lang", "en_us.json"), lang_en),
                               (os.path.join(ASSETS, "lang", "zh_cn.json"), lang_zh)):
        with open(lang_path, encoding="utf-8-sig") as f:
            lang = json.load(f)
        for key,value in entries.items():
            if key.startswith(("block.gregtech.battery_box_","block.gregtech.energy_storage_","block.gregtech.transformer_")):lang[key]=value
            else:lang.setdefault(key,value)
        write(lang_path, dict(sorted(lang.items())))
    print(f"devices: {models}, lang: {len(lang_en)}")


if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--battery-boxes-only",action="store_true")
    parser.add_argument("--storage-and-transformers-only",action="store_true")
    args=parser.parse_args()
    main(args.battery_boxes_only,args.storage_and_transformers_only)
