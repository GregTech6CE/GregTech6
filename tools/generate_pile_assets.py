#!/usr/bin/env python3
"""Generate the ingot / plate / gem-plate / coin pile models, block states and textures.

GT6 renders these four placeables with its own ISBRH, one box per stored item, so a block model has
to bake the geometry instead: every stack size GT6 can show gets its own model file, and the block
state carries the numbers the model was baked for.

  src/main/java/com/gregtech/gregtech/block/misc/PileBlock.java      (STACK 0..64)
  src/main/java/com/gregtech/gregtech/block/misc/CoinPileBlock.java  (COINS 0..16, FILL 1..4)

Geometry, taken from GT6 line by line:

* ingots - ``MultiTileEntityIngot.setBlockBounds`` (gregtech/tileentity/placeables/
  MultiTileEntityIngot.java:52-127) draws one ingot per render pass, eight per layer, alternating a
  layer whose ingots lie along Z with one whose ingots lie along X; pass ``p`` is layer ``p // 8``,
  ingot ``p % 8``. All of GT6's coordinates are 32nds of a block, so the port keeps them exactly
  (half-pixel positions included).
* plates - ``MultiTileEntityPlate.setBlockBounds`` (:52-60, the same geometry in
  ``MultiTileEntityPlateGem.java:52-60``) stacks at most four plates in a 2x2 arrangement, plate ``k``
  reaching ``mSize / 4 (+1)`` sixteenths; only ``min(mSize, 4)`` plates are drawn
  (``getRenderPasses``, :48).
* coins - ``MultiTileEntityCoin.setBlockBounds`` (:396-404), the flat variant GT6 uses while its 3D
  coins are off: one quarter-block coin per face that carries coins (``mCoinStackSizes[i] > 0``), its
  height ``mCoinStackSizes[i] / 16`` (the port quantises that into four steps, see
  ``CoinPileBlock.FILL``).

Textures are GT6's own, copied byte for byte (``textures/blocks/machines/placeables/<kind>/{sides,top}
.png``); the coin reuses the ``COIN``/``COIN_SIDE`` icons the port already carries, which is what
GT6's flat renderer puts on the top, bottom and sides of a coin.

Run from the repository root:  python tools/generate_pile_assets.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import hashlib
import copy
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master"
MANIFEST = ROOT / "tools/gt6_texture_sources.json"
MODELS = ASSETS / "models/block/decoration/pile"
MODEL_ROOT = "gt6ce:block/decoration/pile"

# port texture folder -> GT6 texture folder
PILE_TEXTURES = {"ingot": "ingot", "plate": "plate", "plategem": "plateGem"}

# block id -> (model prefix, port texture folder)
BLOCKS = {
    "ingot_pile": ("ingot", "ingot"),
    "plate_pile": ("plate", "plate"),
    "plate_gem_pile": ("gem_plate", "plategem"),
}


def px(value: float) -> float:
    """GT6's block coordinates are 32nds; block models are in 16ths."""
    return round(value / 2.0, 5)


def element(x0, y0, z0, x1, y1, z1, faces) -> dict:
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def all_faces(top: str, side: str) -> dict:
    return {
        "up": {"texture": top, "tintindex": 0},
        "down": {"texture": top, "tintindex": 0},
        "north": {"texture": side, "tintindex": 0},
        "south": {"texture": side, "tintindex": 0},
        "west": {"texture": side, "tintindex": 0},
        "east": {"texture": side, "tintindex": 0},
    }


def material_faces(kind: str) -> dict:
    """GT6's per-face textures: the top and the bottom of a pile item carry its "top" icon, the four
    horizontal sides its "sides" icon (``getTexture``, MultiTileEntityPlaceable.java:127)."""
    return all_faces(f"gt6ce:block/machines/placeables/{kind}/top",
                     f"gt6ce:block/machines/placeables/{kind}/sides")


def empty_mat(faces: dict) -> list:
    """The one-pixel mat an empty pile draws.

    GT6 cannot have an empty pile - its pile *is* the stored item stack and the multi-tile is removed
    with the last item (``MultiTileEntityPlaceable.java:107``) - but the port's piles are real blocks
    with their own block item, so a freshly placed pile carries nothing yet. Without a mat it would be
    an invisible block; the mat is the port's own addition and uses GT6's own pile textures.
    """
    return [element(0, 0, 0, 16, 1, 16, faces)]


# ── GT6 MultiTileEntityIngot.java:52-127 ────────────────────────────────────────────────────────────

def ingot_elements(stack: int, faces: dict) -> list:
    """The boxes of the first ``stack`` ingots, in GT6's pass order.

    ``pass = layer * 8 + index``: layer ``L`` spans ``PX_P[2L]..PX_P[2L + 2]``; the even layers lie
    along Z (four ingots at z 1..15 and four at 17..31, at x 1..7, 9..15, 17..23, 25..31 in 32nds) and
    the odd layers along X, the same arrangement mirrored.
    """
    elements = []
    for p in range(stack):
        layer, index = divmod(p, 8)
        y0, y1 = layer * 2, layer * 2 + 2
        k, half = index % 4, index // 4
        # The narrow slot repeats every eight 32nds inside each half of the block.
        slot0, slot1 = px(1 + 8 * k), px(7 + 8 * k)
        low0, low1, high0, high1 = px(1), px(15), px(17), px(31)
        if layer % 2 == 0:
            # Long in Z: the narrow slot is x, the half is z.
            z0, z1 = (low0, low1) if half == 0 else (high0, high1)
            elements.append(element(slot0, y0, z0, slot1, y1, z1, faces))
        else:
            # Long in X: the half is x, the narrow slot is z.
            x0, x1 = (low0, low1) if half == 0 else (high0, high1)
            elements.append(element(x0, y0, slot0, x1, y1, slot1, faces))
    return elements


# ── GT6 MultiTileEntityPlate.java:52-60, MultiTileEntityPlateGem.java:52-60 ─────────────────────────

def plate_elements(stack: int, faces: dict) -> list:
    """The boxes of the plates GT6 draws: ``min(stack, 4)`` plates, plate ``k`` as tall as
    ``stack / 4`` sixteenths plus one while ``stack % 4 > k``."""
    elements = []
    for k in range(min(stack, 4)):
        height = stack // 4 + (1 if stack % 4 > k else 0)
        x0, x1 = (px(1), px(15)) if k in (0, 1) else (px(17), px(31))
        z0, z1 = (px(1), px(15)) if k in (0, 2) else (px(17), px(31))
        elements.append(element(x0, 0, z0, x1, height, z1, faces))
    return elements


# ── GT6 MultiTileEntityCoin.java:396-404 (the flat renderer) ────────────────────────────────────────

def coin_elements(coins: int, fill: int, faces: dict) -> list:
    """``coins`` coins in the 4x4 grid, each a quarter block wide and ``fill`` sixteenths high.

    GT6 walks its sixteen counters and draws face ``i`` at ``(i / 4) / 4`` in x and ``(i % 4) / 4`` in
    z (``MultiTileEntityCoin.java:397-403``) with the height ``mCoinStackSizes[i] / 16``; the model
    fills the grid from face 0 upwards, so ``coins`` says how much of the grid carries coins, which is
    what ``CoinPileBlock.COINS`` is set to, and ``fill`` is the quantised height of the fullest face.
    """
    height = fill * 4
    return [element((i // 4) * 4, 0, (i % 4) * 4, (i // 4) * 4 + 4, height, (i % 4) * 4 + 4, faces)
            for i in range(coins)]


COIN_FACES = all_faces("gt6ce:block/iconsets/coin", "gt6ce:block/iconsets/coin_side")


def write_json(path: Path, data, sort: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if sort:
        data = dict(sorted(data.items()))
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def write_model(path: Path, particle: str, elements: list) -> None:
    """One model file: the header pretty printed, one element per line.

    The pile models carry up to 64 elements, so a line per element keeps them readable and the files
    small.
    """
    # BlockModel resolves every face through its texture dictionary. A raw resource
    # location in a face is treated as a missing dictionary key, even if the PNG exists.
    elements = copy.deepcopy(elements)
    textures = {"particle": particle}
    bindings = {}
    for item in elements:
        for face in item["faces"].values():
            texture = face["texture"]
            if texture.startswith("#"):
                continue  # Multiple elements may intentionally share the same face dictionary.
            if texture not in bindings:
                bindings[texture] = "surface_" + str(len(bindings))
                textures[bindings[texture]] = texture
            face["texture"] = "#" + bindings[texture]
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "{",
        '  "parent": "minecraft:block/block",',
        '  "textures": ' + json.dumps(textures, sort_keys=True) + ",",
        '  "elements": [',
    ]
    for index, item in enumerate(elements):
        comma = "," if index < len(elements) - 1 else ""
        lines.append("    " + json.dumps(item, sort_keys=True, separators=(", ", ": ")) + comma)
    lines.append("  ]")
    lines.append("}")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def copy_textures(manifest: dict) -> int:
    copied = 0
    for port_folder, gt6_folder in PILE_TEXTURES.items():
        for name in ("sides", "top"):
            source = (native_path(ORIGINAL / "src/main/resources/assets/gregtech/textures/blocks/machines"
                      / "placeables" / gt6_folder / f"{name}.png"))
            target = ASSETS / "textures/block/machines/placeables" / port_folder / f"{name}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            manifest[target.relative_to(ASSETS).as_posix()] = {
                "source": source.relative_to(ORIGINAL).as_posix(),
                "sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            }
            copied += 1
    return copied


def material_pile(block_id: str, kind: str, folder: str, faces: dict, build) -> int:
    """One of the three material piles: 65 models, its block state and its item model.

    ``kind`` is the model prefix (``gem_plate`` for the gem plates), ``folder`` the port's texture
    folder (``plategem``), which differ for that one family.
    """
    written = 0
    particle = f"gt6ce:block/machines/placeables/{folder}/top"
    variants = {"stack=0": {"model": f"{MODEL_ROOT}/{kind}_0"}}
    write_model(MODELS / f"{kind}_0.json", particle, empty_mat(faces))
    written += 1
    for stack in range(1, 65):
        write_model(MODELS / f"{kind}_{stack}.json", particle, build(stack, faces))
        variants[f"stack={stack}"] = {"model": f"{MODEL_ROOT}/{kind}_{stack}"}
        written += 1
    write_json(ASSETS / f"blockstates/{block_id}.json", {"variants": variants})
    written += 1
    # The item form shows a representative pile, not a full block: one layer of ingots or the four
    # plates of a stack of eight.
    write_json(ASSETS / f"models/item/{block_id}.json", {"parent": f"{MODEL_ROOT}/{kind}_8"})
    written += 1
    return written


def coin_pile() -> int:
    """GT6's coin pile: 64 models plus the empty mat, its block state and its item model."""
    written = 0
    write_model(MODELS / "coin_0.json", "gt6ce:block/iconsets/coin", empty_mat(COIN_FACES))
    written += 1
    variants = {f"coins=0,fill={fill}": {"model": f"{MODEL_ROOT}/coin_0"} for fill in range(1, 5)}
    for coins in range(1, 17):
        for fill in range(1, 5):
            write_model(MODELS / f"coin_{coins}_{fill}.json", "gt6ce:block/iconsets/coin",
                        coin_elements(coins, fill, COIN_FACES))
            variants[f"coins={coins},fill={fill}"] = {"model": f"{MODEL_ROOT}/coin_{coins}_{fill}"}
            written += 1
    write_json(ASSETS / "blockstates/coin_pile.json", {"variants": variants})
    written += 1
    write_json(ASSETS / "models/item/coin_pile.json", {"parent": "gt6ce:item/coin_minted"})
    written += 1
    return written


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    written = copy_textures(manifest)

    for block_id, (kind, port_folder) in BLOCKS.items():
        faces = material_faces(port_folder)
        build = ingot_elements if kind == "ingot" else plate_elements
        written += material_pile(block_id, kind, port_folder, faces, build)
        obsolete = ASSETS / f"models/block/decoration/{block_id}.json"
        if obsolete.exists():
            obsolete.unlink()

    written += coin_pile()
    obsolete = ASSETS / "models/block/decoration/coin_pile.json"
    if obsolete.exists():
        obsolete.unlink()

    write_json(MANIFEST, manifest, sort=True)
    print(f"Wrote {written} files under {ASSETS}")


if __name__ == "__main__":
    main()
