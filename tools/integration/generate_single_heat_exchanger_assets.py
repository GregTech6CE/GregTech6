"""Generate models/groups for the original eight single-block heat exchangers.

Textures are copied unchanged from the preserved GT6 port; material tint is a
native color handler. Runtime parameters and IDs come from HeatExchangerCatalog.
"""
import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "core/src/main/resources/assets/gt6ce"

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--textures", type=Path, help="Preserved assets/gregtech/textures/blocks/machines/generators/hot_fluid directory")
    args = parser.parse_args()
    if args.textures:
        for folder in ("colored", "overlay", "overlay_active"):
            for face in ("front", "left", "top"):
                source = args.textures / folder / (face + ".png")
                target = ASSETS / "textures/block/machines/generators/hot_fluid" / folder / source.name
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, target)
                if source.with_suffix(".png.mcmeta").exists():
                    shutil.copyfile(source.with_suffix(".png.mcmeta"), target.with_suffix(".png.mcmeta"))
    base = "gt6ce:block/machine/heat_exchanger"
    texture = "gt6ce:block/machines/generators/hot_fluid/"
    for active in (False, True):
        for down in (False, True):
            name = "heat_exchanger" + ("_down" if down else "") + ("_active" if active else "")
            faces = {face: "top" if face == "up" else "front" if face == ("down" if down else "north") else "left"
                     for face in ("up", "down", "north", "south", "east", "west")}
            elements = []
            textures = {"particle": texture + "colored/left"}
            for layer, folder in enumerate(("colored", "overlay_active" if active else "overlay")):
                layer_faces = {}
                for face, tex in faces.items():
                    key = str(layer) + tex
                    textures[key] = texture + folder + "/" + tex
                    data = {"texture": "#" + key, "cullface": face}
                    if layer == 0: data["tintindex"] = 0
                    layer_faces[face] = data
                elements.append({"from": [0, 0, 0], "to": [16, 16, 16], "faces": layer_faces})
            write(ASSETS / "models/block/machine" / (name + ".json"), {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": textures, "elements": elements})
    catalog = (ROOT / "core/src/main/java/com/gregtech/gregtech/content/energy/HeatExchangerCatalog.java").read_text(encoding="utf-8")
    ids = re.findall(r'new Spec\("([^"]+)"', catalog)
    for item in ids:
        variants = {}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270), ("down", 0), ("up", 0)):
            for active in (False, True):
                model = base + ("_down" if facing == "down" else "") + ("_active" if active else "")
                variants[f"facing={facing},lit={str(active).lower()}"] = {"model": model, "y": y}
        write(ASSETS / "blockstates" / (item + ".json"), {"variants": variants})
        write(ASSETS / "models/item" / (item + ".json"), {"parent": base})
    for dense in (False, True):
        name = "heat_exchanger_dense" if dense else "heat_exchanger"
        write(ASSETS / "stack_groups/material_forms" / (name + ".json"), {
            "type": "remi:group", "name": "emi.group.gt6ce." + name,
            "regex": "^gt6ce:heat_exchanger" + ("_dense" if dense else "") + "_(?:invar|tungsten|tungsten_steel|ta4hfc5)$", "priority": 110})
    print(f"Generated {len(ids)} single-block heat exchangers")

if __name__ == "__main__": main()
