"""Original GT6 recipes for all flattened long-distance line metadata.

Gregorius Techneticies' Loader_Blocks.java:160-186 and Loader_MultiTileEntities.java:906-914
(read-only source snapshot). LongDistanceCatalog owns the stable identities and material mapping.
Five old voltage IDs remain canonical representatives; the other eleven wire materials and
three fluid pipe materials have their own native blocks. Source networks match metadata.
The ambiguous old long_dist_pipe stays a compatibility block without an invented recipe.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "core/src/main/resources/data/gt6ce"

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def item(name):
    return {"item": "gt6ce:" + name}

def tag(name):
    return {"tag": "gt6ce:" + name}

def recipe(name, pattern, keys):
    write(DATA / f"recipes/long_distance/{name}.json", {
        "type": "gt6ce:tool_shaped", "allow_mirror": False,
        "pattern": pattern, "key": keys, "result": item(name),
    })

def run():
    for family, materials in {
        "plate/any_plastic": ["plastic", "polycarbonate", "pvc", "teflon", "bakelite"],
        "plate_curved/any_copper": ["copper", "annealed_copper"],
    }.items():
        prefix = family.split("/")[0]
        write(DATA / f"tags/items/long_distance/{family}.json", {
            "replace": False,
            "values": [{"id": f"#gt6ce:{prefix}/{material}", "required": False} for material in materials],
        })
    families = {
        "wire_16/any_copper": ["wire_16_copper", "wire_16_annealed_copper"],
        "wire_16/any_steel": ["wire_16_steel", "wire_16_knightmetal", "wire_16_meteoric_steel"],
        "wire_16/any_tungsten": ["wire_16_tungsten", "wire_16_tungsten_sintered"],
        "pipe_medium/any_tungsten": ["pipe_medium_tungsten", "pipe_medium_tungsten_sintered"],
    }
    for family, members in families.items():
        write(DATA / f"tags/items/long_distance/{family}.json", {
            "replace": False, "values": [{"id": "gt6ce:" + name, "required": False} for name in members],
        })
    catalog = (ROOT / "core/src/main/java/com/gregtech/gregtech/content/logistics/LongDistanceCatalog.java").read_text(encoding="utf-8")
    slugs = {"BlueAlloy": "blue_alloy", "ElectrotineAlloy": "electrotine_alloy", "TungstenSteel": "tungsten_steel", "StainlessSteel": "stainless_steel"}
    wires = re.findall(r'wire\("([^"]+)", (\d+), "([^"]+)", (\d+)\)', catalog)
    pipes = re.findall(r'pipe\("([^"]+)", (\d+), "([^"]+)", (-?\d+)\)', catalog)
    assert len(wires) == 17 and len(pipes) == 5, "Source metadata catalog is incomplete"
    for name, meta, material, tier in wires:
        wire = tag("long_distance/wire_16/any_" + material.lower()) if material in ("Copper", "Steel", "Tungsten") else item("wire_16_" + slugs.get(material, material.lower()))
        recipe(name, ["RSR", "PWP", "RSR"], {
            "R": tag("plate/rubber"), "S": tag("plate_curved/aluminium"),
            "P": tag("long_distance/plate_curved/any_copper"), "W": wire,
        })
    for name, meta, material, temperature in pipes:
        pipe = tag("long_distance/pipe_medium/any_tungsten") if material == "Tungsten" else item(("item_pipe_medium_" if int(meta) == 0 else "pipe_medium_") + slugs.get(material, material.lower()))
        recipe(name, ["SPS", "PwP", "SPS"], {
            "S": tag("long_distance/plate/any_plastic"),
            "P": pipe, "w": item("tool_wrench"),
        })
    for name, casing, pipe in [
        ("long_dist_endpoint_item", "platinum", "item_pipe_medium_platinum"),
        ("long_dist_endpoint_fluid", "tungsten", "pipe_medium_tungsten"),
    ]:
        recipe(name, ["ZPZ", "PMP", "ZPZ"], {
            "Z": tag("long_distance/plate/any_plastic"), "P": item(pipe),
            "M": item("casing_machine_" + casing),
        })
    # These first three registry names predate the source alignment; their real grades
    # and translated names are EV, IV, LuV. Keep the IDs for existing saves.
    for suffix, transformer in {
        "ulv": "ev_iv", "lv": "iv_luv", "mv": "luv_zpm", "zpm": "zpm_uv", "uv": "uv_xv",
    }.items():
        recipe("long_dist_transformer_" + suffix, ["WMW", "MxM", "WMW"], {
            "W": item("cable_04_annealed_copper"), "M": item("transformer_" + transformer),
            "x": item("tool_wire_cutter"),
        })
    print("Wrote 29 original-pattern long-distance recipes and 6 shared family tags.")

if __name__ == "__main__":
    run()
