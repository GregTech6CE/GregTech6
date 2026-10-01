"""Generate blockstate and model JSONs for GT6 basic machines."""
import json, os

ROOT = r"F:\Dev\GregTech\GregTech6\src\main\resources\assets\gregtech"
MODELS_DIR = os.path.join(ROOT, "models", "block", "machine", "basic")
BLOCKSTATES_DIR = os.path.join(ROOT, "blockstates")
TEXTURE_BASE = "gregtech:block/machines/basicmachines"

# Standard block-item transforms from vanilla minecraft:block/block
DISPLAY = {
    "gui": {
        "rotation": [30, 225, 0],
        "translation": [0, 0, 0],
        "scale": [0.625, 0.625, 0.625]
    },
    "ground": {
        "rotation": [0, 0, 0],
        "translation": [0, 3, 0],
        "scale": [0.25, 0.25, 0.25]
    },
    "fixed": {
        "rotation": [0, 0, 0],
        "translation": [0, 0, 0],
        "scale": [0.5, 0.5, 0.5]
    },
    "thirdperson_righthand": {
        "rotation": [75, 45, 0],
        "translation": [0, 2.5, 0],
        "scale": [0.375, 0.375, 0.375]
    },
    "firstperson_righthand": {
        "rotation": [0, 45, 0],
        "translation": [0, 0, 0],
        "scale": [0.4, 0.4, 0.4]
    },
    "firstperson_lefthand": {
        "rotation": [0, 225, 0],
        "translation": [0, 0, 0],
        "scale": [0.4, 0.4, 0.4]
    }
}

# ── Material short names (matches GTBasicMachines.MAT_SHORT) ──────────────
MAT_SHORT = {
    "Steel": "steel", "Invar": "invar", "Ti": "titanium",
    "TungstenCarbide": "tungsten_carbide", "Bronze": "bronze",
    "TungstenSteel": "tungsten_steel", "SteelGalvanized": "galvanized_steel",
    "Al": "aluminium", "StainlessSteel": "stainless_steel", "Cr": "chromium",
    "Os": "osmium",
}

# ── Machine definitions (matches GTBasicMachines.MACHINE_DEFS) ────────────
# (name, energy, tier_materials_list)
MACHINES = [
    # HU machines (4 tiers: Steel, Invar, Ti, TungstenCarbide)
    ("oven", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("roaster", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("distillery", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("extruder", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("smelter", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("crystallisationcrucible", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("dryer", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("laminator", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("catalyticcracker", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("steamcracker", ["Steel","Invar","Ti","TungstenCarbide"]),
    # RU machines (4 tiers: Bronze, Steel, Ti, TungstenSteel)
    ("shredder", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("lathe", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("buzzsaw", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("centrifuge", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollingmill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollbender", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollformer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("clustermill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("wiremill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("mixer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("loom", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sluice", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sander", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("burnmixer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("debarker", ["Bronze","Steel","Ti","TungstenSteel"]),
    # KU machines (4 tiers: Bronze, Steel, Ti, TungstenSteel)
    ("crusher", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sifter", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("squeezer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("compressor", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("press", ["Bronze","Steel","Ti","TungstenSteel"]),
    # EU machines (5 tiers: SteelGalvanized, Al, StainlessSteel, Cr, Ti)
    ("electrolyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("canner", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("injector", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("printer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("scannervisuals", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("autocrafter", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricmixer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricloom", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricsifter", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("slicer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("nanofab", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("plantalyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("bumblelyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("boxinator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("unboxinator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    # MU machines (5 tiers)
    ("polarizer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("magneticseparator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    # LU machines (5 tiers)
    ("laserengraver", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("laserwelder", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    # CU machines (5 tiers)
    ("freezer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("cryomixer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    # QU machines (1 tier: Osmium)
    ("massfab", ["Os"]),
    ("scannermolecular", ["Os"]),
    ("replicator", ["Os"]),
    # Single-tier machines (StainlessSteel)
    ("autoclave", ["StainlessSteel"]),
    ("bath", ["StainlessSteel"]),
    ("generifier", ["StainlessSteel"]),
    ("coagulator", ["StainlessSteel"]),
    ("fermenter", ["StainlessSteel"]),
    ("melter", ["StainlessSteel"]),
    ("cokeoven", ["StainlessSteel"]),
    ("lightning", ["StainlessSteel"]),
    ("implosioncompressor", ["StainlessSteel"]),
    ("fusionreactor", ["StainlessSteel"]),
    ("cryodistillationtower", ["StainlessSteel"]),
    ("distillationtower", ["StainlessSteel"]),
]


def model_json(machine_name, overlay="overlay"):
    """Generate the forge:composite model JSON for one machine type."""
    tex_base = f"{TEXTURE_BASE}/{machine_name}"
    return {
        "loader": "forge:composite",
        "display": DISPLAY,
        "children": {
            "layer0": {
                "parent": "minecraft:block/block",
                "textures": {
                    "down": f"{tex_base}/colored/bottom",
                    "up": f"{tex_base}/colored/top",
                    "north": f"{tex_base}/colored/front",
                    "south": f"{tex_base}/colored/back",
                    "west": f"{tex_base}/colored/right",
                    "east": f"{tex_base}/colored/left"
                },
                "elements": [{
                    "from": [0, 0, 0],
                    "to": [16, 16, 16],
                    "faces": {
                        "down":  {"texture": "#down",  "cullface": "down",  "tintindex": 0},
                        "up":    {"texture": "#up",    "cullface": "up",    "tintindex": 0},
                        "north": {"texture": "#north", "cullface": "north", "tintindex": 0},
                        "south": {"texture": "#south", "cullface": "south", "tintindex": 0},
                        "west":  {"texture": "#west",  "cullface": "west",  "tintindex": 0},
                        "east":  {"texture": "#east",  "cullface": "east",  "tintindex": 0}
                    }
                }],
                "render_type": "minecraft:solid"
            },
            "layer1": {
                "parent": "minecraft:block/block",
                "textures": {
                    "down": f"{tex_base}/{overlay}/bottom",
                    "up": f"{tex_base}/{overlay}/top",
                    "north": f"{tex_base}/{overlay}/front",
                    "south": f"{tex_base}/{overlay}/back",
                    "west": f"{tex_base}/{overlay}/right",
                    "east": f"{tex_base}/{overlay}/left"
                },
                "elements": [{
                    "from": [0, 0, 0],
                    "to": [16, 16, 16],
                    "faces": {
                        "down":  {"texture": "#down",  "cullface": "down"},
                        "up":    {"texture": "#up",    "cullface": "up"},
                        "north": {"texture": "#north", "cullface": "north"},
                        "south": {"texture": "#south", "cullface": "south"},
                        "west":  {"texture": "#west",  "cullface": "west"},
                        "east":  {"texture": "#east",  "cullface": "east"}
                    }
                }],
                "render_type": "minecraft:cutout"
            }
        }
    }


def main():
    ITEMS_DIR = os.path.join(ROOT, "models", "item")
    os.makedirs(MODELS_DIR, exist_ok=True)
    os.makedirs(BLOCKSTATES_DIR, exist_ok=True)
    os.makedirs(ITEMS_DIR, exist_ok=True)

    unique_machines = set()
    model_count = 0
    blockstate_count = 0
    item_count = 0

    for name, materials in MACHINES:
        # Generate regular model JSON (overlay, for placed block)
        model_path = os.path.join(MODELS_DIR, f"{name}.json")
        with open(model_path, "w") as f:
            json.dump(model_json(name, "overlay"), f, indent=2)
        model_count += 1

        # Generate active model JSON (overlay_active, for item preview)
        model_path = os.path.join(MODELS_DIR, f"{name}_active.json")
        with open(model_path, "w") as f:
            json.dump(model_json(name, "overlay_active"), f, indent=2)
        model_count += 1

        unique_machines.add(name)

        # Generate blockstate JSON + item model JSON for each variant
        for mat in materials:
            mat_short = MAT_SHORT.get(mat, mat.lower())
            variant_id = f"{name}_{mat_short}"

            # Blockstate — facing+lit variants with y rotation, static overlay for placed block
            blockstate_path = os.path.join(BLOCKSTATES_DIR, f"{variant_id}.json")
            model_ref = f"gregtech:block/machine/basic/{name}"
            blockstate = {
                "variants": {
                    "facing=north,lit=false": {"model": model_ref},
                    "facing=north,lit=true":  {"model": model_ref},
                    "facing=east,lit=false":  {"model": model_ref, "y": 90},
                    "facing=east,lit=true":   {"model": model_ref, "y": 90},
                    "facing=south,lit=false": {"model": model_ref, "y": 180},
                    "facing=south,lit=true":  {"model": model_ref, "y": 180},
                    "facing=west,lit=false":  {"model": model_ref, "y": 270},
                    "facing=west,lit=true":   {"model": model_ref, "y": 270}
                }
            }
            with open(blockstate_path, "w") as f:
                json.dump(blockstate, f, indent=2)
            blockstate_count += 1

            # Item model — uses the active model (animated overlay for inventory/hand)
            item_path = os.path.join(ITEMS_DIR, f"{variant_id}.json")
            item_model = {
                "parent": f"gregtech:block/machine/basic/{name}_active"
            }
            with open(item_path, "w") as f:
                json.dump(item_model, f, indent=2)
            item_count += 1

    print(f"Generated {model_count} model JSONs (static + active for {len(unique_machines)} machine types)")
    print(f"Generated {blockstate_count} blockstate JSONs")
    print(f"Generated {item_count} item model JSONs (pointing to active variants)")


if __name__ == "__main__":
    main()
