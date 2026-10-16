"""Generate the seven GT6 BlockBaseSpike cuboid meshes for Forge block models.

GT6's SpikeRendererYNeg and SpikeRendererOmni each render 13 axis-aligned boxes.
The other five wall facings are rigid rotations of YNeg's boxes. The same
grayscale geometry is used by all five families; a block-color handler supplies
the two original material colours for each family.
"""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "platform-shared/src/main/resources/assets/gregtech"
MODELS = ROOT / "models/block/decoration"
STATES = ROOT / "blockstates"
ITEMS = ROOT / "models/item"
FAMILIES = ("metal", "steel", "sharp", "fancy", "super")


def wall_down_boxes() -> list[tuple[int, int, int, int, int, int]]:
    # gregapi/block/misc/BlockBaseSpike.java:289-315, normal (non-April-Fools) render.
    boxes = [(0, 0, 0, 16, 2, 16)]
    for x in (4, 11):
        for z in (4, 11):
            boxes.append((x, 1, z, x + 1, 15, z + 1))
    for x in (3, 10):
        for z in (3, 10):
            boxes.append((x, 1, z, x + 3, 11, z + 3))
    for x in (2, 9):
        for z in (2, 9):
            boxes.append((x, 1, z, x + 5, 7, z + 5))
    return boxes


def omni_boxes() -> list[tuple[int, int, int, int, int, int]]:
    # BlockBaseSpike.java:376-399: 4-pixel core plus 12 thin spikes to every face.
    boxes = [(4, 4, 4, 12, 12, 12)]
    for a in (5, 10):
        for b in (5, 10):
            boxes.extend(((0, a, b, 16, a + 1, b + 1),
                          (a, 0, b, a + 1, 16, b + 1),
                          (a, b, 0, a + 1, b + 1, 16)))
    return boxes


def rotate_box(box: tuple[int, ...], facing: str) -> tuple[int, ...]:
    x1, y1, z1, x2, y2, z2 = box
    corners = ((x, y, z) for x in (x1, x2) for y in (y1, y2) for z in (z1, z2))
    def rotate(x: int, y: int, z: int) -> tuple[int, int, int]:
        return {
            "down": (x, y, z),
            "up": (x, 16 - y, z),
            "north": (x, z, y),
            "south": (x, z, 16 - y),
            "west": (y, z, x),
            "east": (16 - y, z, x),
        }[facing]
    points = [rotate(*point) for point in corners]
    return tuple(min(point[i] for point in points) for i in range(3)) + tuple(
        max(point[i] for point in points) for i in range(3))


def model(boxes: list[tuple[int, ...]]) -> dict:
    return {
        "textures": {
            "all": "gregtech:block/machines/multiblockparts/metalwall/0/colored/side",
            "particle": "#all",
        },
        "elements": [
            {
                "from": list(box[:3]),
                "to": list(box[3:]),
                "faces": {face: {"texture": "#all", "tintindex": 0} for face in
                          ("down", "up", "north", "south", "west", "east")},
            }
            for box in boxes
        ],
    }


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for facing in ("down", "up", "north", "south", "west", "east"):
        write(MODELS / f"spike_wall_{facing}.json",
              model([rotate_box(box, facing) for box in wall_down_boxes()]))
    write(MODELS / "spike_omni.json", model(omni_boxes()))

    multipart = [
        {"when": {"mode": "wall", "facing": facing},
         "apply": {"model": f"gregtech:block/decoration/spike_wall_{facing}"}}
        for facing in ("down", "up", "north", "south", "west", "east")
    ]
    multipart.extend({"when": {"mode": mode},
                      "apply": {"model": "gregtech:block/decoration/spike_omni"}}
                     for mode in ("omni", "falling"))
    for family in FAMILIES:
        write(STATES / f"spike_{family}.json", {"multipart": multipart})
        write(ITEMS / f"spike_{family}.json",
              {"parent": "gregtech:block/decoration/spike_omni"})


if __name__ == "__main__":
    main()
