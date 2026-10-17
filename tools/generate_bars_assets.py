"""Generate the four-bit GT6 bars geometry for every registered material.

The twenty world cuboids and six inventory cuboids follow BlockBaseBars' render
passes.  A model contains only the passes enabled by its north/south/west/east
mask, so connected segments render without overlapping duplicate rails.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "platform-shared/src/main/resources/assets/gregtech"
MATERIALS = (
    "iron", "steel", "brass", "bronze", "wrought_iron", "stainless", "tungsten_steel"
)
TEXTURE = "gregtech:block/machines/multiblockparts/metalwall/0/colored/side"

# Bit combinations for GT6's render passes 0..19.  The first four are corner
# posts; each wall contributes its bottom/top rails and two inner uprights.
PASSES = (
    (5, (0, 0, 0), (1, 16, 1)),
    (9, (15, 0, 0), (16, 16, 1)),
    (6, (0, 0, 15), (1, 16, 16)),
    (10, (15, 0, 15), (16, 16, 16)),
    (1, (1, 0, 0), (15, 1, 1)),
    (4, (0, 0, 1), (1, 1, 15)),
    (2, (1, 0, 15), (15, 1, 16)),
    (8, (15, 0, 1), (16, 1, 15)),
    (1, (1, 15, 0), (15, 16, 1)),
    (4, (0, 15, 1), (1, 16, 15)),
    (2, (1, 15, 15), (15, 16, 16)),
    (8, (15, 15, 1), (16, 16, 15)),
    (1, (5, 1, 0), (6, 15, 1)),
    (4, (0, 1, 5), (1, 15, 6)),
    (2, (5, 1, 15), (6, 15, 16)),
    (8, (15, 1, 5), (16, 15, 6)),
    (1, (10, 1, 0), (11, 15, 1)),
    (4, (0, 1, 10), (1, 15, 11)),
    (2, (10, 1, 15), (11, 15, 16)),
    (8, (15, 1, 10), (16, 15, 11)),
)
ITEM_PASSES = (
    ((0, 0, 7), (1, 16, 8)),
    ((15, 0, 7), (16, 16, 8)),
    ((1, 0, 7), (15, 1, 8)),
    ((1, 15, 7), (15, 16, 8)),
    ((5, 1, 7), (6, 15, 8)),
    ((10, 1, 7), (11, 15, 8)),
)


def element(lo: tuple[int, int, int], hi: tuple[int, int, int]) -> dict:
    return {
        "from": list(lo), "to": list(hi),
        "faces": {face: {"texture": "#all", "tintindex": 0} for face in
                  ("down", "up", "north", "south", "west", "east")},
    }


def model(elements: list[dict]) -> dict:
    return {
        "parent": "minecraft:block/block",
        "ambientocclusion": False,
        "textures": {"all": TEXTURE, "particle": TEXTURE},
        "elements": elements,
    }


def output_files() -> dict[Path, dict]:
    result: dict[Path, dict] = {}
    for name in MATERIALS:
        blockstate = {"variants": {}}
        base = ROOT / "models/block/decoration"
        for mask in range(16):
            elements = [element(lo, hi) for bits, lo, hi in PASSES if mask & bits]
            if mask == 0:
                elements = [element(lo, hi) for lo, hi in ITEM_PASSES]
            result[base / f"bars_{name}_{mask}.json"] = model(elements)
            blockstate["variants"][f"mask={mask}"] = {
                "model": f"gregtech:block/decoration/bars_{name}_{mask}"
            }
        result[ROOT / f"blockstates/bars_{name}.json"] = blockstate
        result[base / f"bars_{name}.json"] = model(
            [element(lo, hi) for lo, hi in ITEM_PASSES]
        )
        result[ROOT / f"models/item/bars_{name}.json"] = {
            "parent": f"gregtech:block/decoration/bars_{name}"
        }
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    outputs = output_files()
    stale: list[str] = []
    for path, payload in outputs.items():
        rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
        if args.check:
            if not path.is_file() or path.read_text(encoding="utf-8") != rendered:
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(rendered, encoding="utf-8")
    if stale:
        raise SystemExit("Outdated bars assets: " + ", ".join(stale))
    print(f"GT6 bars assets: {len(outputs)} files {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()
