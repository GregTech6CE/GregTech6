#!/usr/bin/env python3
"""Generate the blockstates and models for GT6's tracks (`Loader_Rails:41-72`).

GT6's three rail families are registered by `registry/GTTrackBlocks.java`; this writes the client
side for them:

* one blockstate per track, following vanilla's `rail.json` / `powered_rail.json` /
  `detector_rail.json` shape (and `powered` on/off) variants,
* the per-material model stubs that re-texture vanilla's rail geometry with the GT6 iconset
  texture of that material (`assets/gregtech/textures/block/iconsets/rail_*.png`, already in the
  repo). Vanilla's rail models take the texture variable `rail`, so every stub is a one-liner.

Inputs : none (the table below mirrors `GTTrackBlocks.java` / GT6 `Loader_Rails:41-72`)
Output : platform-shared/src/main/resources/assets/gregtech/blockstates/track*.json
         platform-shared/src/main/resources/assets/gregtech/models/block/tracks/*.json
         platform-shared/src/main/resources/assets/gregtech/models/item/track*.json
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BLOCKSTATES = ROOT / "platform-shared/src/main/resources/assets/gregtech/blockstates"
MODELS = ROOT / "platform-shared/src/main/resources/assets/gregtech/models/block/tracks"
ITEM_MODELS = ROOT / "platform-shared/src/main/resources/assets/gregtech/models/item"

# material slug -> the GT6 iconset texture stem (upper case, as in the resource pack)
# `LoadTracks.java` uses RAIL_STRAIGHT_/RAIL_TURNED_/RAIL_BOOSTER(_ACTIVE)_/RAIL_DETECTOR(_ACTIVE)_.
MATERIALS = ["aluminium", "bronze", "magnalium", "steel", "stainlesssteel", "tungsten",
             "titanium", "tungstensteel", "tungstencarbide", "adamantium"]

# Vanilla shape -> (model suffix, y rotation); vanilla's own rail blockstates, verbatim.
STRAIGHT_SHAPES = [
    ("ascending_east", "raised_ne", 90),
    ("ascending_north", "raised_ne", 0),
    ("ascending_south", "raised_sw", 0),
    ("ascending_west", "raised_sw", 90),
    ("east_west", "flat", 90),
    ("north_east", "curved", 270),
    ("north_south", "flat", 0),
    ("north_west", "curved", 180),
    ("south_east", "curved", 0),
    ("south_west", "curved", 90),
]
# Powered/detector rails have no curves.
POWERED_SHAPES = [
    ("ascending_east", "raised_ne", 90),
    ("ascending_north", "raised_ne", 0),
    ("ascending_south", "raised_sw", 0),
    ("ascending_west", "raised_sw", 90),
    ("east_west", "flat", 90),
    ("north_south", "flat", 0),
]

# shape suffix -> the vanilla parent that carries the geometry
PARENTS = {
    "flat": "minecraft:block/rail_flat",
    "curved": "minecraft:block/rail_curved",
    "raised_ne": "minecraft:block/template_rail_raised_ne",
    "raised_sw": "minecraft:block/template_rail_raised_sw",
    # powered/detector "on" variants reuse vanilla's own recoloured stubs
    "flat_on": "minecraft:block/rail_flat",
    "raised_ne_on": "minecraft:block/template_rail_raised_ne",
    "raised_sw_on": "minecraft:block/template_rail_raised_sw",
}


def model(name: str, shape: str, texture_stem: str) -> None:
    """Write one re-textured rail model (1.20.1 resource paths are lower case)."""
    parent = PARENTS[shape]
    MODELS.mkdir(parents=True, exist_ok=True)
    (MODELS / (name + ".json")).write_text(json.dumps({
        "parent": parent,
        # Rails cut the texture out; without it the transparent pixels render black.
        "render_type": "minecraft:cutout",
        "textures": {"rail": "gregtech:block/iconsets/" + texture_stem.lower()},
    }, indent=2) + "\n", encoding="utf-8", newline="\n")


def blockstate(name: str, variants: dict) -> None:
    (BLOCKSTATES / (name + ".json")).write_text(
        json.dumps({"variants": variants}, indent=2) + "\n", encoding="utf-8", newline="\n")


def item_model(name: str) -> None:
    """Use the idle flat track sprite as its inventory preview."""
    ITEM_MODELS.mkdir(parents=True, exist_ok=True)
    (ITEM_MODELS / (name + ".json")).write_text(json.dumps({
        "parent": "gregtech:block/tracks/" + name + "_flat",
    }, indent=2) + "\n", encoding="utf-8", newline="\n")


def variant(model_name: str, y: int) -> dict:
    entry = {"model": "gregtech:block/tracks/" + model_name}
    if y:
        entry["y"] = y
    return entry


def main() -> int:
    written = 0
    for slug in MATERIALS:
        upper = slug.upper()

        # ── plain track ──────────────────────────────────────────────────
        variants = {}
        for shape, kind, y in STRAIGHT_SHAPES:
            stem = "RAIL_TURNED_" + upper if kind == "curved" else "RAIL_STRAIGHT_" + upper
            name = "track_%s_%s" % (slug, kind)
            model(name, kind, stem)
            written += 1
            variants["shape=" + shape] = variant(name, y)
        blockstate("track_" + slug, variants)
        item_model("track_" + slug)

        # ── booster / detector track ─────────────────────────────────────
        for family, off, on in (("booster", "RAIL_BOOSTER_%s", "RAIL_BOOSTER_ACTIVE_%s"),
                                ("detector", "RAIL_DETECTOR_%s", "RAIL_DETECTOR_ACTIVE_%s")):
            variants = {}
            for shape, kind, y in POWERED_SHAPES:
                for powered, stem in ((False, off % upper), (True, on % upper)):
                    suffix = kind if not powered else kind + "_on"
                    name = "track_%s_%s_%s" % (family, slug, suffix)
                    model(name, suffix, stem)
                    written += 1
                    key = "powered=%s,shape=%s" % ("true" if powered else "false", shape)
                    variants[key] = variant(name, y)
            blockstate("track_%s_%s" % (family, slug), variants)
            item_model("track_%s_%s" % (family, slug))

    print("wrote %d block models, %d blockstates and %d item models for %d tracks"
          % (written, len(MATERIALS) * 3, len(MATERIALS) * 3, len(MATERIALS) * 3))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
