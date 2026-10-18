"""Write the machine-component crafting recipes the port was missing.

GT6 defines these in `MultiItemTechnological.java` (the item class registers its own crafting
recipes when the item is added): one shaped recipe per voltage tier for the compact motors, pumps,
conveyors, pistons and robot arms (`:405-423`), plus the field generators, signal emitters and
sensors (`:425-460`). The port shipped the ULV..LuV rows only, so ZPM/UV/PUV1 (and the ULV rows of
the four families that start at LV) had no recipe at all - see `docs/items-without-recipes.json`.

Tier arrays used by the original:
  * `MT.DATA.Electric_T` = TinAlloy, SteelGalvanized, Al, StainlessSteel, Cr, Ti, Ir, Os, Trinitanium, Trinaquadalloy
  * `MT.DATA.CABLES_01`  = Pb, Sn, Cu, Au, Al, Pt, Graphene x4   (the port registers graphene as a wire)
  * `MT.DATA.WIRES_01`   = same materials, 1x wire
  * `MT.DATA.WIRES_04`   = same materials, 4x wire
  * `OD_CIRCUITS`        = the selector tags `gt:circuitN` -> the port's `integrated_circuit_N`

Usage: python tools/generate_missing_component_recipes.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT_DIR = os.path.join(ROOT, str(native_path("src")), "main", "resources", "data", "gt6ce", "recipes", "components")

# MT.DATA.Electric_T (MT.java:3690) - the port's material *name* per GT6 voltage tier
ELECTRIC_T_NAME = ["TinAlloy", "SteelGalvanized", "Aluminium", "StainlessSteel", "Chromium",
                   "Titanium", "Iridium", "OsmiumElemental", "Trinitanium", "Trinaquadalloy"]
# item ids use the lower-cased registry name (TinAlloy -> tinalloy), Forge tag paths the snake_cased
# one (MaterialEquivalence.materialName: TinAlloy -> tin_alloy, OsmiumElemental -> osmium_elemental)
ELECTRIC_T = [n.lower() for n in ELECTRIC_T_NAME]
TAG_NAME = {n.lower(): __import__("re").sub(r"([a-z0-9])([A-Z])", r"\1_\2", n).lower()
            for n in ELECTRIC_T_NAME}
TAG_NAME["netherquartz"] = "quartz"
# MT.DATA.CABLES_01 / WIRES_01 / WIRES_04 (MT.java:3594-3646)
CABLES_01 = ["lead", "tin", "copper", "gold", "aluminium", "platinum",
             "graphene", "graphene", "graphene", "graphene"]
# GT6 MT.DATA.CABLES_01 itself uses OP.wireGt01 for Graphene; it is not a port fallback.
CABLE_ITEM = {m: ("wire_01_" + m if m == "graphene" else "cable_01_" + m) for m in set(CABLES_01)}
# OD_CIRCUITS[0..9]: the port's selector tags
CIRCUIT = {i: f"integrated_circuit_{i}" for i in range(10)}
# the field generators / emitters / sensors pick their gem per tier (MultiItemTechnological:425-460)
GEM = {0: {"tag": "forge:gems/quartz"}, 1: {"tag": "forge:gems/quartz"}, 2: {"tag": "forge:gems/quartz"},
       3: {"item": "minecraft:emerald"}, 4: {"item": "minecraft:ender_pearl"},
       5: {"item": "minecraft:ender_eye"}, 6: {"item": "minecraft:nether_star"},
       7: {"item": "minecraft:nether_star"}, 8: {"item": "minecraft:nether_star"},
       9: {"item": "minecraft:nether_star"}}
# the field generators use a different gem list (enderman drops earlier)
FIELD_GEM = {0: {"item": "minecraft:ender_pearl"}, 1: {"item": "minecraft:ender_pearl"},
             2: {"item": "minecraft:ender_eye"}, 3: {"item": "minecraft:ender_eye"},
             4: {"item": "minecraft:nether_star"}, 5: {"item": "minecraft:nether_star"},
             6: {"item": "minecraft:nether_star"}, 7: {"item": "minecraft:nether_star"},
             8: {"item": "minecraft:nether_star"}, 9: {"item": "minecraft:nether_star"}}
# the field generators walk 1/2/4/6/8/10/12/14/16x osmium wire (MultiItemTechnological:425-434)
FIELD_WIRE_SIZE = [0, 1, 2, 4, 6, 8, 10, 12, 14, 16]
# MT.DATA.WIRES_01 / CABLES_01 indexes per tier for the emitters and sensors
TIER_NAME = ["ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1"]

TIER_MISSING = [7, 8, 9]          # ZPM, UV, PUV1
ULV_MISSING = ["robot_arm", "field_emitter", "signal_emitter", "sensor"]

def tag(material: str, group: str) -> dict:
    """Forge tag key for a material form, using the port's snake_cased tag path."""
    return {"tag": f"forge:{group}/{TAG_NAME.get(material, material)}"}


ROD = lambda m: tag(m, "rods")                      # noqa: E731
SCREW = lambda m: tag(m, "screws")                  # noqa: E731
PLATE = lambda m: tag(m, "plates")                  # noqa: E731
CURVED = lambda m: {"item": f"gt6ce:plate_curved_{m}"}       # noqa: E731
DOUBLE = lambda m: {"item": f"gt6ce:plate_double_{m}"}       # noqa: E731
WIRE = lambda size, m: {"item": f"gt6ce:wire_{size:02d}_{m}"}   # noqa: E731
CABLE = lambda m: {"item": f"gt6ce:{CABLE_ITEM[m]}"}            # noqa: E731
GEAR_SMALL = lambda m: {"item": f"gt6ce:gear_gt_small_{m}"}     # noqa: E731
ROTOR = lambda m: {"item": f"gt6ce:rotor_{m}"}                  # noqa: E731
ITEM = lambda i: {"item": f"gt6ce:{i}"}                         # noqa: E731

RUBBER_RING = {"tag": "forge:rings/rubber"}
RUBBER_PLATE = {"tag": "forge:plates/rubber"}
SCREWDRIVER = ITEM("tool_screwdriver")
WRENCH = ITEM("tool_wrench")


def motor(i: int) -> dict:
    """MultiItemTechnological:405-416 - wire size grows with the tier."""
    wire = {"item": "gt6ce:wire_fine_copper"} if i == 0 else WIRE(i, "annealed_copper" if i >= 4 else "copper")
    magnet = {"tag": "forge:bolts/iron_magnetic"} if i == 0 else (
        ROD("iron_magnetic") if i == 1 else ROD("neodymium_magnetic") if i < 6 else {"tag": "forge:long_rods/neodymium_magnetic"})
    return shaped("minecraft:crafting_shaped", ["CWR", "WIW", "PWC"], {
        "C": CABLE(CABLES_01[i]), "W": wire, "R": ROD(ELECTRIC_T[i]),
        "P": CURVED(ELECTRIC_T[i]), "I": magnet}, f"compact_electric_motor_{TIER_NAME[i]}")


def pump(i: int) -> dict:
    return shaped("gt6ce:tool_shaped", ["TXO", "dPw", "OMT"], {
        "T": SCREW(ELECTRIC_T[i]), "X": ROTOR(ELECTRIC_T[i]), "O": RUBBER_RING,
        "d": SCREWDRIVER, "w": WRENCH, "P": CURVED(ELECTRIC_T[i]),
        "M": ITEM(f"compact_electric_motor_{TIER_NAME[i]}")}, f"compact_electric_pump_{TIER_NAME[i]}")


def conveyor(i: int) -> dict:
    return shaped("minecraft:crafting_shaped", ["RRR", "MCM", "RRR"], {
        "R": RUBBER_PLATE, "M": ITEM(f"compact_electric_motor_{TIER_NAME[i]}"),
        "C": CABLE(CABLES_01[i])}, f"compact_electric_conveyor_{TIER_NAME[i]}")


def piston(i: int) -> dict:
    return shaped("gt6ce:tool_shaped", ["TPP", "dSS", "TMG"], {
        "T": SCREW(ELECTRIC_T[i]), "P": PLATE(ELECTRIC_T[i]), "d": SCREWDRIVER,
        "S": ROD(ELECTRIC_T[i]), "M": ITEM(f"compact_electric_motor_{TIER_NAME[i]}"),
        "G": GEAR_SMALL(ELECTRIC_T[i])}, f"compact_electric_piston_{TIER_NAME[i]}")


def robot_arm(i: int) -> dict:
    return shaped("minecraft:crafting_shaped", ["CCC", "MSM", "PES"], {
        "C": CABLE(CABLES_01[i]), "M": ITEM(f"compact_electric_motor_{TIER_NAME[i]}"),
        "S": ROD(ELECTRIC_T[i]), "P": ITEM(f"compact_electric_piston_{TIER_NAME[i]}"),
        "E": ITEM(CIRCUIT[i])}, f"compact_robot_arm_{TIER_NAME[i]}")


def field_emitter(i: int) -> dict:
    """MultiItemTechnological:425-434 - wireFine(Os) at ULV, then 1/2/4/6/…/16x wire for the rest."""
    # the port calls GT6's Os "OsmiumElemental", and its material items use that name
    wire = {"item": "gt6ce:wire_fine_osmiumelemental"} if i == 0 else WIRE(FIELD_WIRE_SIZE[i], "osmium")
    return shaped("minecraft:crafting_shaped", ["WPW", "CGC", "WPW"], {
        "W": wire, "P": DOUBLE(ELECTRIC_T[i]), "C": ITEM(CIRCUIT[i]),
        "G": FIELD_GEM[i]}, f"compact_force_field_emitter_{TIER_NAME[i]}")


def signal_emitter(i: int) -> dict:
    """MultiItemTechnological:436-445 - 4x wire and a 1x cable of the tier's material."""
    return shaped("minecraft:crafting_shaped", ["SPC", "WQP", "CWS"], {
        "S": WIRE(4, CABLES_01[i]), "P": CURVED(ELECTRIC_T[i]), "C": ITEM(CIRCUIT[i]),
        "W": CABLE(CABLES_01[i]), "Q": GEM[i]}, f"compact_signal_emitter_{TIER_NAME[i]}")


def sensor(i: int) -> dict:
    """MultiItemTechnological:447-456 - 1x wire of the tier's material."""
    return shaped("minecraft:crafting_shaped", ["P Q", "PS ", "CPP"], {
        "P": CURVED(ELECTRIC_T[i]), "Q": GEM[i], "S": WIRE(1, CABLES_01[i]),
        "C": ITEM(CIRCUIT[i])}, f"compact_sensor_{TIER_NAME[i]}")


def shaped(recipe_type: str, pattern: list[str], key: dict, result: str) -> dict:
    return {"type": recipe_type, "pattern": pattern, "key": key,
            "result": {"item": f"gt6ce:{result}", "count": 1}}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report without writing")
    args = parser.parse_args()

    written: list[str] = []
    skipped: list[str] = []
    for i in TIER_MISSING:
        for family, builder in (("motor", motor), ("pump", pump), ("conveyor", conveyor),
                                ("piston", piston), ("robot_arm", robot_arm),
                                ("field_emitter", field_emitter), ("signal_emitter", signal_emitter),
                                ("sensor", sensor)):
            name = f"{family}_{TIER_NAME[i]}"
            path = os.path.join(OUT_DIR, name + ".json")
            if os.path.exists(path):
                skipped.append(name)
                continue
            if not args.check:
                with open(path, "w", encoding="utf-8", newline="\n") as handle:
                    json.dump(builder(i), handle, indent=2)
                    handle.write("\n")
            written.append(name)
    for family, builder in (("robot_arm", robot_arm), ("field_emitter", field_emitter),
                            ("signal_emitter", signal_emitter), ("sensor", sensor)):
        name = f"{family}_ulv"
        path = os.path.join(OUT_DIR, name + ".json")
        if os.path.exists(path):
            skipped.append(name)
            continue
        if not args.check:
            with open(path, "w", encoding="utf-8", newline="\n") as handle:
                json.dump(builder(0), handle, indent=2)
                handle.write("\n")
        written.append(name)

    print(f"{'would write' if args.check else 'wrote'} {len(written)} recipes: {', '.join(written)}")
    if skipped:
        print(f"already present ({len(skipped)}): {', '.join(skipped)}")
    print("NOTE: tiers above PUV1 (PUV2..XV) have no crafting recipe in GT6 either"
          " (VN[] has 16 names, the crafting loops stop at index 9).")


if __name__ == "__main__":
    main()
