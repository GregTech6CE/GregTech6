#!/usr/bin/env python3
"""Build only the 10 GT6 powered magnet models from original GT6 textures."""

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GT6 = ROOT.parent / "gregtech6-master" / "gregtech6-master" / "src" / "main" / "resources" / "assets" / "gregtech" / "textures" / "blocks" / "machines"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "gregtech"
TEXTURES = ASSETS / "textures" / "block" / "machines"
TIERS = ("lv", "mv", "hv", "ev", "iv")


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def layer(folder, overlay):
    subdir = "colored" if overlay is None else overlay
    names = {"north": "front", "south": "back", "west": "side", "east": "side", "up": "side", "down": "side"}
    textures = {side: f"gregtech:block/machines/magnets/{folder}/{subdir}/{name}" for side, name in names.items()}
    faces = {side: {"texture": "#" + side, **({"tintindex": 0} if overlay is None else {})}
             for side in names}
    return {"parent": "minecraft:block/block", "textures": textures,
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
            "render_type": "minecraft:cutout"}


def model(folder, active):
    return {"parent": "minecraft:block/block", "loader": "forge:composite",
            "children": {"colored": layer(folder, None),
                         "overlay": layer(folder, "overlay_active" if active else "overlay")}}


def blockstate(name):
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                 "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    variants = {}
    for facing, rotation in rotations.items():
        for active in (False, True):
            variants[f"facing={facing},active={str(active).lower()}"] = {
                "model": f"gregtech:block/machine/energy/{name}{'_active' if active else ''}", **rotation}
    return {"variants": variants}


def main():
    for folder in ("magnet_electric", "magnet_flux"):
        for subdir in ("colored", "overlay", "overlay_active"):
            for name in ("front", "back", "side"):
                source = GT6 / "magnets" / folder / subdir / f"{name}.png"
                if not source.is_file():
                    raise FileNotFoundError(source)
                dest = TEXTURES / "magnets" / folder / subdir / source.name
                dest.parent.mkdir(parents=True, exist_ok=True)
                if not dest.exists():
                    shutil.copy2(source, dest)
    for tier in TIERS:
        for name, folder in ((f"electromagnet_{tier}", "magnet_electric"),
                             (f"flux_magnet_{tier}", "magnet_flux")):
            for active in (False, True):
                model_name = name + ("_active" if active else "")
                write_json(ASSETS / "models" / "block" / "machine" / "energy" / f"{model_name}.json",
                           model(folder, active))
            write_json(ASSETS / "blockstates" / f"{name}.json", blockstate(name))
            write_json(ASSETS / "models" / "item" / f"{name}.json",
                       {"parent": f"gregtech:block/machine/energy/{name}"})
    # Recipes are resolved once from core OriginalMagnetCrafting by both native
    # loaders. Never recreate the old unguarded static JSON recipes here.


if __name__ == "__main__":
    main()
