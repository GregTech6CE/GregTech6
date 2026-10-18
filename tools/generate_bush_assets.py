"""Copy GT6's berry bush textures and generate the stage models, the blockstate and the language
keys of the port's bush block.

GT6's bush is a tinted multi-layer render: a greyscale ``colored`` layer that the block colour
handler tints per stage plus a greyscale ``overlay`` detail layer. The four stages use
bush -> bush+immature -> bush+berries -> bush+berries (the last two differ only in the tint colour,
which the colour handler picks from GT6's BushesGT table, so they share one model).
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import os
import shutil

GT6_TEXTURES = os.path.join("..", "gregtech6-master", "gregtech6-master", "build", "resources", "main",
                            "assets", "gregtech", "textures", "blocks", "machines", "plants", "bush")
ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gregtech")
TEX_OUT = os.path.join(ASSETS, "textures", "block", "plants", "bush")
LANG = os.path.join(ASSETS, "lang")

TEXTURES = [
    "colored/bush.png",
    "colored/berries.png",
    "colored/berries_immature.png",
    "overlay/bush.png",
    "overlay/berries.png",
    "overlay/berries_immature.png",
]

# GT6's stage -> berry texture (None = no berry layer yet)
STAGES = {
    0: None,
    1: "berries_immature",
    2: "berries",
    3: "berries",
}

FACES = ("north", "south", "east", "west", "up", "down")
BUSH_BOX = ([2, 0, 2], [14, 4, 14])
BERRY_BOX = ([2.1, 0.1, 2.1], [13.9, 4.1, 13.9])

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


def cube(box, texture, tint):
    low, high = box
    return {
        "from": low,
        "to": high,
        "faces": {face: ({"texture": texture, "tintindex": tint} if tint is not None
                         else {"texture": texture}) for face in FACES},
    }


def layer(berry, tinted):
    base = "gregtech:block/plants/bush/colored/" if tinted else "gregtech:block/plants/bush/overlay/"
    elements = [cube(BUSH_BOX, "#bush", 0 if tinted else None)]
    if berry:
        elements.append(cube(BERRY_BOX, "#berry", 1 if tinted else None))
    textures = {"bush": base + "bush"}
    if berry:
        textures["berry"] = base + berry
    return {"textures": textures, "elements": elements, "render_type": "minecraft:cutout"}


def model(stage):
    berry = STAGES[stage]
    return {
        "parent": "minecraft:block/block",
        "loader": "forge:composite",
        "textures": {"particle": "gregtech:block/plants/bush/colored/bush"},
        "children": {
            "colored": layer(berry, True),
            "overlay": layer(berry, False),
        },
    }


os.makedirs(TEX_OUT, exist_ok=True)
for relative in TEXTURES:
    src = os.path.join(GT6_TEXTURES, relative.replace("/", os.sep))
    if not os.path.exists(src):
        raise SystemExit("missing GT6 texture: " + src)
    dst = os.path.join(TEX_OUT, relative.replace("/", os.sep))
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    written.append(os.path.relpath(dst, ASSETS).replace("\\", "/"))

for stage in STAGES:
    write_json(os.path.join(ASSETS, "models", "block", "plants", "bush_stage%d.json" % stage), model(stage))
write_json(os.path.join(ASSETS, "models", "item", "bush.json"), {"parent": "gregtech:block/plants/bush_stage3"})
write_json(os.path.join(ASSETS, "blockstates", "bush.json"), {"variants": {
    "stage=%d" % stage: {"model": "gregtech:block/plants/bush_stage%d" % stage} for stage in STAGES}})

for name, value in (("en_us.json", "Berry Bush"), ("zh_cn.json", "浆果灌木丛")):
    path = os.path.join(LANG, name)
    with open(path, encoding="utf-8") as handle:
        lines = handle.readlines()
    if any('"block.gregtech.bush"' in line for line in lines):
        print("%s: bush key already present" % name)
        continue
    anchor = next((i for i, line in enumerate(lines) if '"block.gregtech.rock"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor for the bush key" % name)
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    lines[anchor:anchor] = ['%s"block.gregtech.bush": "%s",\n' % (indent, value)]
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.writelines(lines)
    print("%s: inserted the bush key" % name)

print("textures: %d, assets: %d" % (len(TEXTURES), len(written)))
for rel in written:
    print("  " + rel)
