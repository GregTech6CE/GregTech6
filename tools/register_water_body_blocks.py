"""Give GT6's ocean / river / swamp water fluids a world block (Forge `LiquidBlock`).

GT6's `WorldgenOcean` / `WorldgenRiver` / `WorldgenSwamp` (Loader_Worldgen:576-578) turn the water
of oceans, rivers and swamps into GT6's own `BlocksGT.Ocean` / `River` / `Swamp` fluid blocks, so
those three fluids need a `LiquidBlock` in the port (same machinery the fluid springs got in §44).

GT6 renders every waterlike block with **vanilla water's icon** (`BlockWaterlike.getIcon` returns
`Blocks.water.getIcon`, `colorMultiplier` = white), so the three blocks reuse vanilla water's
textures — no new art.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import os

LOADER = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "bootstrap", "content", "Loader_Fluids.java")
ASSETS = os.path.join(str(native_path("src")), "main", "resources", "assets", "gt6ce")
LANG = os.path.join(ASSETS, "lang")

# path (GT6 fluid registry name) -> (english, chinese)
FLUIDS = {
    "seawater": ("Sea Water", "海水"),
    "riverwater": ("River Water", "河水"),
    "swampwater": ("Swamp Water", "沼泽水"),
}

OLD_SET = '''    /**
     * Fluids that also exist as world blocks — GT6's fluid springs ({@code WorldgenFluidSpring},
     * Loader_Worldgen:782-797) place oil, natural gas, geothermal water and lava into the world, so
     * those six fluids need a {@link LiquidBlock}.
     */
    private static final Set<String> SPRING_FLUID_PATHS = Set.of(
            "liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil",
            "gas_natural_gas", "watergeothermal");'''

NEW_SET = '''    /**
     * Fluids that also exist as world blocks. GT6's fluid springs ({@code WorldgenFluidSpring},
     * Loader_Worldgen:782-797) place oil, natural gas and geothermal water into the world, and
     * GT6's ocean/river/swamp passes ({@code WorldgenOcean} / {@code WorldgenRiver} /
     * {@code WorldgenSwamp}, Loader_Worldgen:576-578) replace the water of oceans, rivers and
     * swamps with GT6's own water fluids — all of those need a {@link LiquidBlock}.
     */
    private static final Set<String> WORLD_FLUID_PATHS = Set.of(
            "liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil", "liquid_light_oil",
            "gas_natural_gas", "watergeothermal",
            "seawater", "riverwater", "swampwater");'''

written = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(obj, handle, indent=2)
        handle.write("\n")
    written.append(os.path.relpath(path, ASSETS).replace("\\", "/"))


text = io.open(LOADER, encoding="utf-8").read()
if "WORLD_FLUID_PATHS" in text:
    print("Loader_Fluids: already wired")
else:
    assert text.count(OLD_SET) == 1, "fluid set anchor"
    text = text.replace(OLD_SET, NEW_SET)
    assert text.count("SPRING_FLUID_PATHS.contains(path)") == 1, "usage anchor"
    text = text.replace("SPRING_FLUID_PATHS.contains(path)", "WORLD_FLUID_PATHS.contains(path)")
    io.open(LOADER, "w", encoding="utf-8", newline="\n").write(text)
    print("Loader_Fluids: water body fluids now get a LiquidBlock")

# Blockstate + models: a LiquidBlock uses vanilla water's 16 level variants, and GT6 renders its
# waterlike blocks with vanilla water's icon, so the models reference vanilla water textures.
STILL = "minecraft:block/water_still"
FLOW = "minecraft:block/water_flow"
for path in FLUIDS:
    variants = {"level=%d" % level: {
        "model": "gt6ce:block/fluid/%s%s" % (path, "" if level == 0 else "_flow")}
        for level in range(16)}
    write_json(os.path.join(ASSETS, "blockstates", path + ".json"), {"variants": variants})
    for suffix in ("", "_flow"):
        write_json(os.path.join(ASSETS, "models", "block", "fluid", path + suffix + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"particle": STILL, "still": STILL, "flow": FLOW},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    "down": {"texture": "#still", "cullface": "down"},
                    "up": {"texture": "#still", "cullface": "up"},
                    "north": {"texture": "#flow", "cullface": "north"},
                    "south": {"texture": "#flow", "cullface": "south"},
                    "west": {"texture": "#flow", "cullface": "west"},
                    "east": {"texture": "#flow", "cullface": "east"}}},
            ]})

for name, index in (("en_us.json", 0), ("zh_cn.json", 1)):
    file = os.path.join(LANG, name)
    lines = io.open(file, encoding="utf-8").readlines()
    anchor = next((i for i, line in enumerate(lines) if '"block.gt6ce.loot_crate"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor" % name)
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    inserts = []
    for path, values in FLUIDS.items():
        key = '"block.gt6ce.%s"' % path
        if any(key in line for line in lines):
            continue
        inserts.append('%s%s: "%s",\n' % (indent, key, values[index]))
    lines[anchor:anchor] = inserts
    if inserts:
        io.open(file, "w", encoding="utf-8", newline="\n").writelines(lines)
    print("%s: %d water block keys" % (name, len(inserts)))

print("wrote %d asset files" % len(written))
