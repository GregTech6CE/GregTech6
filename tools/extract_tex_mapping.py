#!/usr/bin/env python3
"""Extract material->texture_folder mapping from build directory models."""
import json
from pathlib import Path

BUILD = Path("F:/Dev/GregTech/GregTech6/build/resources/main/assets/gt6ce/models/block/machine")

MATERIALS = [
    'wood','treated_wood','plastic','rubber','copper','aluminium','tin_alloy',
    'bronze','invar','steel','galvanized_steel','hsla','gold','chrome',
    'stainless_steel','vanadium_steel','desh','tungsten_alloy','tungsten_steel',
    'tungsten_carbide','desh_alloy','palladium','carbon','tantalum_hafnium_carbide',
    'titanium','tungsten','efrine','netherite','iridium','ironwood','thaumium',
    'manasteel','void_metal','terrasteel','gaia_spirit','bedrock_hsla',
    'adamantium','draconium','awakened_draconium','infinity',
]

for mat in MATERIALS:
    path = BUILD / f"pipe_medium_{mat}_core.json"
    if path.exists():
        d = json.loads(path.read_text(encoding="utf-8"))
        tex = list(d["textures"].values())[0]
        folder = tex.split("/")[-2]
        print(f'    "{mat}": "{folder}",')
    else:
        print(f"    # {mat}: MISSING")
