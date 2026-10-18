"""Generate the datapack recipes for GT6's covers, USB parts, logistics buses and selector circuits.

Every row below is transcribed from the original's own registration code, with the file:line kept in
the recipe's ``comment`` field so the provenance survives into the generated JSON:

  * covers              ``gregtech/items/MultiItemTechnological.java:139-178`` (blank/crafting/drain/
                        vent/warning/retriever/pressure valve) and ``:419-422`` (the pump/conveyor/
                        robot-arm covers, which GT6 registers inside the component loop)
  * USB parts           ``MultiItemTechnological.java:796-799`` (sticks), ``:808-811`` (cables),
                        ``:819-822`` (HDDs)
  * logistics           ``MultiItemTechnological.java:773-787`` (shaped rows), ``:117-131`` (the
                        shapeless variant cycle that GT6 ships alongside them)
  * selector circuits   ``gregapi/item/ItemIntegratedCircuit.java:58-85`` (variant 0 plus the 24
                        "programming" steps that pick a tag)

Key mapping notes (see also ``MachineRecipeIngredients`` and ``rebuild_panel_covers.py``):

  * ``OD_CIRCUITS[n]`` is GT6's ``gt:circuitN``, made cumulative by
    ``LoaderOreDictReRegistrations:375-383``; the port spells the tiers ``circuit_basic`` …
    ``circuit_ultimate``, so tier n resolves to every tier up to n.
  * ``MT.DATA.CABLES_01[n]`` / ``WIRES_01[n]``: 0 = lead, 1 = tin, 2 = copper, 3 = gold,
    4 = aluminium, 5 = platinum, 6 = graphene.
  * ``MT.Cr`` is the port's Chromium (``ElementMaterials.Chromium``), ``MT.Os`` its OsmiumElemental.
  * ``OP.wireFine.dat(MT.Os)`` is the port's ``wire_fine_osmiumelemental``.
  * ``OD.itemGlue`` covers slime (``LoaderOreDictReRegistrations:969-972``) -> ``forge:slimeballs``;
    1.20.1's ``forge:slimeballs`` is the slime tag.
  * ``IL.Circuit_Selector`` (the programmed circuit used as a cover, ``ItemIntegratedCircuit:87``)
    becomes the port's ``cover_tag_selector``; the vanilla torch/repeater that GT6 registers as
    covers directly (``GT_API:799-802``) become the port's two cover items.

Run:  python tools/generate_covers_usb_logistics_recipes.py
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "core/src/main/resources/data/gt6ce"

SHAPED = "minecraft:crafting_shaped"
SHAPELESS = "minecraft:crafting_shapeless"
TOOL_SHAPED = "gt6ce:tool_shaped"

TOOL = {
    "w": "gt6ce:tool_wrench",
    "d": "gt6ce:tool_screwdriver",
    "h": "gt6ce:tool_hammer",
    "x": "gt6ce:tool_wire_cutter",
    "r": "gt6ce:tool_soft_hammer",
}


def item(i: str) -> dict:
    return {"item": i}


def tag(t: str) -> dict:
    return {"tag": t}


def circuits(tier: int) -> dict:
    """GT6 high-to-low re-registration accepts tier N and every higher tier."""
    return tag("gt6ce:circuits_tier_" + str(tier) + "_plus")


# ── covers (MultiItemTechnological:139-178, :419-422) ─────────────────────────────────────────────

# IL.PISTONS[1] - the item retriever cover uses the LV piston (MultiItemTechnological:170)
LV_PISTON = "gt6ce:compact_electric_piston_lv"

COVERS = [
    # id, gt6, type, pattern, key
    ("crafting_table_cover", "MultiItemTechnological:140", SHAPED,
     ["C", "Q"],
     {"C": item("minecraft:crafting_table"), "Q": item("gt6ce:blank_cover")}),
    # GT6's row is "GB"/"YQ" but never defines 'B' (CR.shaped would leave the slot empty), so the
    # port crafts the cover from the glue and the dye that GT6 does define.
    ("warning_cover", "MultiItemTechnological:167", SHAPED,
     ["G ", "YQ"],
     {"G": tag("forge:slimeballs"), "Y": tag("forge:dyes/yellow"),
      "Q": item("gt6ce:blank_cover")}),
    ("item_retriever_cover", "MultiItemTechnological:170", SHAPED,
     ["RPR", "CQC"],
     {"R": item("gt6ce:plate_curved_electrum"), "P": item(LV_PISTON),
      "C": circuits(3), "Q": item("gt6ce:item_filter")}),
    ("drain", "MultiItemTechnological:159", TOOL_SHAPED,
     ["RRR", "RwR", "RRR"],
     {"R": tag("forge:rods/iron"), "w": item(TOOL["w"])}),
    ("air_vent", "MultiItemTechnological:161", SHAPED,
     ["RRR", "RXR", "RRR"],
     {"R": tag("forge:rods/iron"), "X": item("gt6ce:rotor_iron")}),
    ("pressure_value", "MultiItemTechnological:178", TOOL_SHAPED,
     ["TCT", "wPd"],
     {"T": tag("forge:screws/brass"), "C": item("gt6ce:plate_curved_brass"),
      "P": item("gt6ce:plate_brass"), "w": item(TOOL["w"]), "d": item(TOOL["d"])}),
]

# IL.PUMPS / IL.CONVEYERS / IL.ROBOT_ARMS (MultiItemTechnological:419-422): GT6's compact electric
# pump, conveyor and robot arm *are* the covers, so the port registers no cover shell for them and
# the components keep the original's LV recipes (components/pump_lv.json, conveyor_lv.json,
# robot_arm_lv.json). CoverItems maps every tier onto the cover behaviour.
COVER_FROM_COMPONENT = []

# GT6 makes the vanilla torch/repeater the cover itself (GT_API:799-802) and the programmed circuit
# the tag selector (ItemIntegratedCircuit:87); the compact electric pump/conveyor/robot arm double as
# covers (MultiItemTechnological:50-53). The port follows that model (content/cover/CoverItems), so
# none of those five families needs a recipe of its own here - the vanilla items, the compact
# components and the selector circuits are used directly on the machine face.
COVER_SHAPELESS = []

# ── USB parts (MultiItemTechnological:796-799, :808-811, :819-822) ────────────────────────────────

USB_TIERS = [
    # index, wire/cable tier material, component tier cable, plate/screw material, circuit tier
    (1, "gold", "gold", "aluminium", 3),
    (2, "aluminium", "aluminium", "stainless_steel", 4),
    (3, "platinum", "platinum", "chromium", 5),
    (4, "graphene", "graphene", "titanium", 6),
]

USB = []
for index, wire, cable, plate, tier in USB_TIERS:
    USB.append((f"usb{index}_stick", f"MultiItemTechnological:{795 + index}", TOOL_SHAPED,
                ["xWd", "PCP", "TCT"],
                {"x": item(TOOL["x"]), "d": item(TOOL["d"]),
                 "W": item(f"gt6ce:wire_01_{wire}"), "C": circuits(tier),
                 "P": tag(f"forge:plates/{plate}"), "T": tag(f"forge:screws/{plate}")}))
for index, wire, cable, plate, tier in USB_TIERS:
    USB.append((f"usb{index}_cable", f"MultiItemTechnological:{807 + index}", TOOL_SHAPED,
                ["xWd", "PCP", "TCT"],
                {"x": item(TOOL["x"]), "d": item(TOOL["d"]),
                 "W": item(f"gt6ce:wire_01_{wire}"), "C": item(f"gt6ce:{'wire' if cable == 'graphene' else 'cable'}_01_{cable}"),
                 "P": tag(f"forge:plates/{plate}"), "T": tag(f"forge:screws/{plate}")}))
for index, wire, cable, plate, tier in USB_TIERS:
    USB.append((f"usb{index}_hdd", f"MultiItemTechnological:{818 + index}", TOOL_SHAPED,
                ["PLT", "dRW", "TCP"],
                {"P": tag(f"forge:plates/{plate}"), "L": item("gt6ce:laser_emitter_helium"),
                 "T": tag(f"forge:screws/{plate}"), "d": item(TOOL["d"]),
                 "R": tag("minecraft:music_discs"), "W": item(f"gt6ce:usb{index}_cable"),
                 "C": circuits(tier)}))

# ── logistics (MultiItemTechnological:773-787 shaped, :117-131 shapeless cycle) ───────────────────

BUS_KEYS = {"Q": item("gt6ce:blank_cover"),
            "P": item("gt6ce:crystal_processor_emerald"),
            "C": circuits(4),
            "W": item("gt6ce:wire_fine_osmiumelemental")}

LOGISTICS = [
    ("logistics_display_cpu_logic", "MultiItemTechnological:773", TOOL_SHAPED,
     ["dL ", " Q ", " C "],
     {"d": item(TOOL["d"]), "L": item("gt6ce:wire_01_lumium"),
      "Q": item("gt6ce:blank_cover"), "C": circuits(2)}),
    ("logistics_display_cpu_control", "MultiItemTechnological:774", TOOL_SHAPED,
     [" Ld", " Q ", " C "],
     {"d": item(TOOL["d"]), "L": item("gt6ce:wire_01_lumium"),
      "Q": item("gt6ce:blank_cover"), "C": circuits(2)}),
    ("logistics_display_cpu_storage", "MultiItemTechnological:775", TOOL_SHAPED,
     [" L ", " Q ", "dC "],
     {"d": item(TOOL["d"]), "L": item("gt6ce:wire_01_lumium"),
      "Q": item("gt6ce:blank_cover"), "C": circuits(2)}),
    ("logistics_display_cpu_conversion", "MultiItemTechnological:776", TOOL_SHAPED,
     [" L ", " Q ", " Cd"],
     {"d": item(TOOL["d"]), "L": item("gt6ce:wire_01_lumium"),
      "Q": item("gt6ce:blank_cover"), "C": circuits(2)}),
    ("filtered_logistics_export_bus_fluid", "MultiItemTechnological:778", TOOL_SHAPED,
     ["  w", "WQW", "CPC"], dict(BUS_KEYS, w=item(TOOL["w"]))),
    ("filtered_logistics_import_bus_fluid", "MultiItemTechnological:779", TOOL_SHAPED,
     [" w ", "WQW", "CPC"], dict(BUS_KEYS, w=item(TOOL["w"]))),
    ("filtered_logistics_storage_bus_fluid", "MultiItemTechnological:780", TOOL_SHAPED,
     ["w  ", "WQW", "CPC"], dict(BUS_KEYS, w=item(TOOL["w"]))),
    ("filtered_logistics_export_bus_item", "MultiItemTechnological:781", TOOL_SHAPED,
     ["  r", "WQW", "CPC"], dict(BUS_KEYS, r=item(TOOL["r"]))),
    ("filtered_logistics_import_bus_item", "MultiItemTechnological:782", TOOL_SHAPED,
     [" r ", "WQW", "CPC"], dict(BUS_KEYS, r=item(TOOL["r"]))),
    ("filtered_logistics_storage_bus_item", "MultiItemTechnological:783", TOOL_SHAPED,
     ["r  ", "WQW", "CPC"], dict(BUS_KEYS, r=item(TOOL["r"]))),
    ("generic_logistics_export_bus", "MultiItemTechnological:784", TOOL_SHAPED,
     ["  d", "WQW", "CPC"], dict(BUS_KEYS, d=item(TOOL["d"]))),
    ("generic_logistics_import_bus", "MultiItemTechnological:785", TOOL_SHAPED,
     [" d ", "WQW", "CPC"], dict(BUS_KEYS, d=item(TOOL["d"]))),
    ("generic_logistics_storage_bus", "MultiItemTechnological:786", TOOL_SHAPED,
     ["d  ", "WQW", "CPC"], dict(BUS_KEYS, d=item(TOOL["d"]))),
    ("logistics_dump_bus_item", "MultiItemTechnological:787", SHAPED,
     ["   ", "WQW", "CPC"], BUS_KEYS),
]

# GT6 ships these next-to-each-other conversions so a placed design can be switched in place.
LOGISTICS_CYCLE = [
    ("logistics_cycle_cpu_logic", "MultiItemTechnological:117",
     "logistics_display_cpu_logic", "logistics_display_cpu_conversion"),
    ("logistics_cycle_cpu_control", "MultiItemTechnological:118",
     "logistics_display_cpu_control", "logistics_display_cpu_logic"),
    ("logistics_cycle_cpu_storage", "MultiItemTechnological:119",
     "logistics_display_cpu_storage", "logistics_display_cpu_control"),
    ("logistics_cycle_cpu_conversion", "MultiItemTechnological:120",
     "logistics_display_cpu_conversion", "logistics_display_cpu_storage"),
    ("logistics_cycle_fluid_export", "MultiItemTechnological:122",
     "filtered_logistics_export_bus_fluid", "logistics_dump_bus_item"),
    ("logistics_cycle_fluid_import", "MultiItemTechnological:123",
     "filtered_logistics_import_bus_fluid", "filtered_logistics_export_bus_fluid"),
    ("logistics_cycle_fluid_storage", "MultiItemTechnological:124",
     "filtered_logistics_storage_bus_fluid", "filtered_logistics_import_bus_fluid"),
    ("logistics_cycle_item_export", "MultiItemTechnological:125",
     "filtered_logistics_export_bus_item", "filtered_logistics_storage_bus_fluid"),
    ("logistics_cycle_item_import", "MultiItemTechnological:126",
     "filtered_logistics_import_bus_item", "filtered_logistics_export_bus_item"),
    ("logistics_cycle_item_storage", "MultiItemTechnological:127",
     "filtered_logistics_storage_bus_item", "filtered_logistics_import_bus_item"),
    ("logistics_cycle_generic_export", "MultiItemTechnological:128",
     "generic_logistics_export_bus", "filtered_logistics_storage_bus_item"),
    ("logistics_cycle_generic_import", "MultiItemTechnological:129",
     "generic_logistics_import_bus", "generic_logistics_export_bus"),
    ("logistics_cycle_generic_storage", "MultiItemTechnological:130",
     "generic_logistics_storage_bus", "generic_logistics_import_bus"),
    ("logistics_cycle_dump", "MultiItemTechnological:131",
     "logistics_dump_bus_item", "generic_logistics_storage_bus"),
]

# ── selector circuits (ItemIntegratedCircuit:58-85) ──────────────────────────────────────────────

# variant -> the screwdriver's position in the 3x3 grid, exactly as GT6 writes it.
PROGRAMMING = {
    1: ["d ", " P"], 2: ["d ", "P "], 3: [" d", "P "], 4: ["Pd", "  "],
    5: ["P ", " d"], 6: ["P ", "d "], 7: [" P", "d "], 8: ["dP", "  "],
    9: ["P d", "   ", "   "], 10: ["P  ", "  d", "   "], 11: ["P  ", "   ", "  d"],
    12: ["P  ", "   ", " d "], 13: ["  P", "   ", "  d"], 14: ["  P", "   ", " d "],
    15: ["  P", "   ", "d  "], 16: ["  P", "d  ", "   "], 17: ["   ", "   ", "d P"],
    18: ["   ", "d  ", "  P"], 19: ["d  ", "   ", "  P"], 20: [" d ", "   ", "  P"],
    21: ["d  ", "   ", "P  "], 22: [" d ", "   ", "P  "], 23: ["  d", "   ", "P  "],
    24: ["   ", "  d", "P  "],
}


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != text:
        path.write_text(text, encoding="utf-8")


def entry(gt6: str, kind: str, pattern: list, key: dict,
          result: str, shapeless: list | None = None) -> dict:
    recipe = {"type": kind, "_comment": f"GT6 {gt6}"}
    if shapeless is None:
        recipe["pattern"] = pattern
        recipe["key"] = key
        # mirroring would make two different GT6 rows indistinguishable (see the programming rows)
        if kind == TOOL_SHAPED:
            recipe["allow_mirror"] = False
    else:
        recipe["ingredients"] = shapeless
    recipe["result"] = {"item": f"gt6ce:{result}"}
    return recipe


def generated() -> dict:
    files = {}
    for result, gt6, kind, pattern, key in COVERS:
        files[DATA / f"recipes/hand_covers/{result}.json"] = entry(
            gt6, kind, pattern, key, result)
    for result, gt6, shapeless in COVER_SHAPELESS:
        files[DATA / f"recipes/hand_covers/{result}.json"] = entry(
            gt6, SHAPELESS, [], {}, result, shapeless)
    for result, gt6, component in COVER_FROM_COMPONENT:
        files[DATA / f"recipes/hand_covers/{result}.json"] = entry(
            gt6, SHAPELESS, [], {}, result, [item("gt6ce:" + component)])
    for result, gt6, kind, pattern, key in USB:
        files[DATA / f"recipes/hand_components/{result}.json"] = entry(
            gt6, kind, pattern, key, result)
    for result, gt6, kind, pattern, key in LOGISTICS:
        files[DATA / f"recipes/logistics/{result}.json"] = entry(
            gt6, kind, pattern, key, result)
    for recipe_id, gt6, result, source in LOGISTICS_CYCLE:
        files[DATA / f"recipes/logistics/{recipe_id}.json"] = entry(
            gt6, SHAPELESS, [], {}, result, [item("gt6ce:" + source)])
    files[DATA / "recipes/hand_components/integrated_circuit_0.json"] = entry(
        "ItemIntegratedCircuit:58", TOOL_SHAPED,
        ["GhG", "SSS", "GwG"],
        {"G": item("gt6ce:gear_gt_small_iron"), "S": tag("forge:rods/iron"),
         "h": item(TOOL["h"]), "w": item(TOOL["w"])},
        "integrated_circuit_0")
    for variant, pattern in PROGRAMMING.items():
        files[DATA / f"recipes/hand_components/integrated_circuit_{variant}.json"] = entry(
            f"ItemIntegratedCircuit:{60 + variant - 1}",
            TOOL_SHAPED, pattern,
            {"P": tag("gt6ce:integrated_circuits"), "d": item(TOOL["d"])},
            f"integrated_circuit_{variant}")
    return files


def main() -> None:
    files = generated()
    for path, data in files.items():
        write(path, data)
    # The selector tag: GT6's wildcard variant ("any programmed circuit") as a real tag file.
    write(DATA / "tags/items/integrated_circuits.json",
          {"replace": False,
           "values": [f"gt6ce:integrated_circuit_{i}" for i in range(25)]})
    groups = {}
    for path in files:
        groups[path.parent.name] = groups.get(path.parent.name, 0) + 1
    print(f"wrote {len(files)} recipes: " + ", ".join(f"{k}={v}" for k, v in sorted(groups.items())))
    print("wrote tags/items/integrated_circuits.json (25 selector circuits)")


if __name__ == "__main__":
    main()
