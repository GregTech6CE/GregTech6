#!/usr/bin/env python3
"""Restore static multipart arms for pipes/wires/cables.

The baseline shipped core-only blockstates and drew every connection arm in a
BlockEntityRenderer, which loses AO/lighting and z-fights. This rewrites the
blockstates to core + per-direction side models (skipping full-cube cores) so
the BER only has to draw the short extension into a thicker neighbor.

Also patches wire/cable side models to cullface their end cap (the pipe side
models already do), so caps vanish inside solid machine faces.
"""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
BLOCKSTATES = SHARED_ASSETS / "blockstates"
MODELS_MACHINE = SHARED_ASSETS / "models/block/machine"

PIPE_RE = re.compile(r"^(item_)?pipe_(tiny|small|medium|large|huge|quadruple|nonuple)_(.+)\.json$")
WIRE_RE = re.compile(r"^(wire|cable)_(\d{2})_(.+)\.json$")

ARM_PARTS = [
    ("north", {"y": 0}),
    ("south", {"y": 180}),
    ("east",  {"y": 90}),
    ("west",  {"y": 270}),
    ("up",    {"x": 270}),
    ("down",  {"x": 90}),
]


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def is_full_cube(model_path: Path) -> bool:
    try:
        model = read_json(model_path)
    except FileNotFoundError:
        return False
    elements = []
    if "elements" in model:
        elements = model["elements"]
    elif model.get("loader") == "forge:composite":
        for child in model.get("children", {}).values():
            elements.extend(child.get("elements", []))
    return any(e.get("from") == [0, 0, 0] and e.get("to") == [16, 16, 16] for e in elements)


def multipart(core_ref: str, side_ref: str):
    parts = [{"apply": {"model": core_ref}}]
    for prop, rot in ARM_PARTS:
        apply = {"model": side_ref}
        apply.update(rot)
        parts.append({"when": {prop: "true"}, "apply": apply})
    return {"multipart": parts}


def patch_side_cullface(model_path: Path) -> bool:
    """Ensure the north (end-cap) face of a side model culls against the neighbor."""
    try:
        model = read_json(model_path)
    except FileNotFoundError:
        return False
    changed = False
    for element in model.get("elements", []):
        north = element.get("faces", {}).get("north")
        if north is not None and north.get("cullface") != "north":
            north["cullface"] = "north"
            changed = True
    if changed:
        write_json(model_path, model)
    return changed


def main():
    pipes = wires = full = culled = 0
    for bs in sorted(BLOCKSTATES.glob("*.json")):
        m = PIPE_RE.match(bs.name)
        if m:
            size, mat = m.group(2), m.group(3)
            base = f"pipe_{size}_{mat}"
        else:
            m = WIRE_RE.match(bs.name)
            if not m:
                continue
            kind, nn = m.group(1), m.group(2)
            base = f"{kind}_{nn}"
        core = MODELS_MACHINE / f"{base}_core.json"
        side = MODELS_MACHINE / f"{base}_side.json"
        core_ref = f"gregtech:block/machine/{base}_core"
        side_ref = f"gregtech:block/machine/{base}_side"
        if is_full_cube(core) or not side.exists():
            write_json(bs, {"variants": {"": {"model": core_ref}}})
            full += 1
            continue
        if patch_side_cullface(side):
            culled += 1
        write_json(bs, multipart(core_ref, side_ref))
        if PIPE_RE.match(bs.name):
            pipes += 1
        else:
            wires += 1

    print(f"multipart pipes: {pipes}, wires/cables: {wires}, "
          f"core-only (full cube): {full}, side models cullface-patched: {culled}")


if __name__ == "__main__":
    main()
