#!/usr/bin/env python3
"""Transpile GT6 stone layer worldgen data into the port.

Sources (original GT6, LGPL):
  - gregtech/worldgen/NoiseGenerator.java  -> CELL_3D table for GTCellNoise
  - gregtech/loaders/b/Loader_Worldgen.java -> StoneLayer.LAYERS list (order matters:
    the cellular noise indexes into it) and bothsides/topbottom contact-ore pairs.

Outputs:
  - src/.../worldgen/GTCellNoise.java       (noise incl. the 256-entry cell table)
  - src/.../worldgen/GTStoneLayersGen.java  (layer + contact data)

Cross-mod alternate blocks (ST.block(MD.X, ...)) are dropped: the port resolves the
stone via material name -> StoneType, then the BlocksGT fallback, then vanilla stone.
Ores guarded by MD.<mod>.mLoaded are dropped (those mods don't exist on 1.20.1).
Biome filters are dropped for now (TODO).
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import os
import re

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java"
ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen")

NOISE_SRC = os.path.join(GT6, "gregtech", "worldgen", "NoiseGenerator.java")
WORLDGEN_SRC = os.path.join(GT6, "gregtech", "loaders", "b", "Loader_Worldgen.java")

# BlocksGT field -> port StoneType enum constant
BLOCKSGT_TO_STONE = {
    "Basalt": "BASALT", "GraniteBlack": "GRANITE_BLACK", "GraniteRed": "GRANITE_RED",
    "Marble": "MARBLE", "Limestone": "LIMESTONE", "Granite": "GRANITE",
    "Diorite": "DIORITE", "Andesite": "ANDESITE", "Komatiite": "KOMATIITE",
    "Greenschist": "GREENSCHIST", "Blueschist": "BLUESCHIST", "Kimberlite": "KIMBERLITE",
    "Quartzite": "QUARTZITE", "Slate": "SLATE", "Shale": "SHALE",
    "PrismarineLight": "PRISMARINE_LIGHT", "PrismarineDark": "PRISMARINE_DARK",
}
# material name -> StoneType (the stones added in Wave 7 have no BlocksGT block)
MATERIAL_TO_STONE = {
    "Basalt": "BASALT", "GraniteBlack": "GRANITE_BLACK", "GraniteRed": "GRANITE_RED",
    "Marble": "MARBLE", "Limestone": "LIMESTONE", "Granite": "GRANITE",
    "Diorite": "DIORITE", "Andesite": "ANDESITE", "Komatiite": "KOMATIITE",
    "Greenschist": "GREENSCHIST", "Blueschist": "BLUESCHIST", "Kimberlite": "KIMBERLITE",
    "Quartzite": "QUARTZITE", "Slate": "SLATE", "Shale": "SHALE",
    "PrismarineLight": "PRISMARINE_LIGHT", "PrismarineDark": "PRISMARINE_DARK",
    "Chalk": "CHALK", "Dolomite": "DOLOMITE", "Gabbro": "GABBRO", "Gneiss": "GNEISS",
    "Gypsum": "GYPSUM", "Oilshale": "OILSHALE", "Rhyolite": "RHYOLITE",
    "NaCl": "SALT", "KCl": "SYLVITE", "Talc": "TALC",
}

CHANCE = {"U": 1, "U2": 2, "U3": 3, "U4": 4, "U5": 5, "U6": 6, "U8": 8, "U9": 9,
          "U12": 12, "U16": 16, "U24": 24, "U32": 32, "U48": 48, "U64": 64,
          "U72": 72, "U96": 96, "U128": 128}


def balanced(text: str, start: int) -> str:
    """Return the text of the call starting at the '(' at/after `start`."""
    i = text.index("(", start)
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "(":
            depth += 1
        elif text[j] == ")":
            depth -= 1
            if depth == 0:
                return text[i + 1:j]
    raise ValueError("unbalanced")


def split_args(body: str) -> list:
    out, depth, cur = [], 0, []
    for ch in body:
        if ch in "(<[":
            depth += 1
        elif ch in ")>]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        out.append("".join(cur).strip())
    return out


def mt_name(expr: str):
    m = re.match(r"MT\.(?:STONES\.|OREMATS\.|WOODS\.)?(\w+)$", expr.strip())
    if m:
        return m.group(1)
    if expr.strip().startswith("MT.UNUSED."):
        return None
    return None


def parse_ore(arg: str):
    """StoneLayerOres(...) -> (material, indicators, chanceDiv, minY, maxY) or None."""
    arg = arg.strip()
    if not arg or arg == "null":
        return None
    if ".mLoaded" in arg:  # cross-mod conditional
        return None
    # strip `cond ? new StoneLayerOres(...) : null` keeping the call when cond is not mod-gating
    m = re.search(r"new StoneLayerOres\s*\(", arg)
    if not m:
        return None
    body = balanced(arg, m.end() - 1)
    args = split_args(body)
    if not args:
        return None
    mat = mt_name(args[0])
    if mat is None:
        return None
    idx = 1
    indicators = True
    if idx < len(args) and args[idx] in ("T", "F"):
        indicators = args[idx] == "T"
        idx += 1
    if idx >= len(args):
        return None
    chance = CHANCE.get(args[idx].replace(" ", ""))
    if chance is None:
        return None
    idx += 1
    try:
        min_y = int(args[idx]); max_y = int(args[idx + 1])
    except (ValueError, IndexError):
        return None
    return (mat, indicators, chance, min_y, max_y)


def parse_layer(body: str):
    args = split_args(body)
    stone = None
    material = None
    ores = []
    for i, arg in enumerate(args):
        arg = arg.strip()
        if i == 0:
            m = re.match(r"BlocksGT\.(\w+)$", arg)
            if m:
                stone = BLOCKSGT_TO_STONE.get(m.group(1))
            continue
        if i == 1 and arg.startswith("MT."):
            material = mt_name(arg)
            continue
        if "StoneLayerOres" in arg:
            ore = parse_ore(arg)
            if ore:
                ores.append(ore)
    if material and material in MATERIAL_TO_STONE:
        stone = MATERIAL_TO_STONE[material]
    if material is None and stone:
        for mat, st in MATERIAL_TO_STONE.items():
            if st == stone:
                material = mat
                break
    return (stone, material or "Stone", ores)


# GT6 BlockRockOres ("Dense Ores that typically generate in large Layers") adds its
# own StoneLayers in the block constructor — meta -> (iconset block id, material).
# These are whole-block ore seams (anthracite coal seams, salt domes, oil shale, ...).
# Nether quartz seams are Nether-only (GTNetherDepositFeature), not overworld layers.
ROCK_ORE_LAYERS = [
    ("block_ore_anthracite", "Coal"),
    ("block_ore_lignite", "Lignite"),
    ("block_ore_salt", "NaCl"),
    ("block_ore_rocksalt", "KCl"),
    ("block_ore_bauxite", "Bauxite"),
    ("block_ore_oil", "Oilshale"),
    ("block_ore_gypsum", "Gypsum"),
    ("block_ore_milkyquartz", "MilkyQuartz"),
]


def main() -> None:
    src = open(WORLDGEN_SRC, encoding="utf-8").read()

    layers = []
    for m in re.finditer(r"StoneLayer\.LAYERS\.add\(\s*new StoneLayer\s*\(", src):
        body = balanced(src, m.end() - 1)
        stmt_end = src.index(";", m.end())
        no_deep = ".setNoDeep()" in src[m.start():stmt_end]
        stone, material, ores = parse_layer(body)
        layers.append((stone, None, material, no_deep, ores))
    # Rock-ore seam layers registered by BlockRockOres (always NoDeep in GT6).
    layers = [(None, bid, mat, True, []) for bid, mat in ROCK_ORE_LAYERS] + layers

    contacts = []
    for m in re.finditer(r"StoneLayer\.(bothsides|topbottom)\s*\(", src):
        body = balanced(src, m.end() - 1)
        args = split_args(body)
        if len(args) < 3:
            continue
        a, b = mt_name(args[0]), mt_name(args[1])
        if a is None or b is None:
            continue
        ores = [o for o in (parse_ore(x) for x in args[2:]) if o]
        if ores:
            contacts.append((m.group(1) == "bothsides", a, b, ores))

    noise_src = open(NOISE_SRC, encoding="utf-8").read()
    cells = re.findall(r"new Float3\(([-0-9.eE]+)f,\s*([-0-9.eE]+)f,\s*([-0-9.eE]+)f\)", noise_src)
    assert len(cells) == 256, f"expected 256 cell entries, got {len(cells)}"

    write_noise(cells)
    write_layers(layers, contacts)
    n_ores = sum(len(l[4]) for l in layers)
    print(f"layers: {len(layers)} ({n_ores} layer ores), contacts: {len(contacts)}, cells: {len(cells)}")


def write_noise(cells) -> None:
    rows = []
    for i in range(0, 256, 4):
        row = ", ".join(f"{x}f, {y}f, {z}f" for x, y, z in cells[i:i + 4])
        rows.append("            " + row + ",")
    table = "\n".join(rows)
    content = f"""package com.gregtech.gregtech.worldgen;

/**
 * Cellular (Worley) noise, ported from GT6 {{@code gt6ce.worldgen.NoiseGenerator}}.
 * Horizontal frequency 0.009 (~110-block regions), vertical 0.075 (~13-block strata):
 * this is what makes stone layers vary regionally instead of by fixed height bands.
 *
 * <p>GENERATED by tools/transpile_gt6_stonelayers.py — do not edit by hand.</p>
 */
public final class GTCellNoise {{
    private static final int X_PRIME = 1619, Y_PRIME = 31337, Z_PRIME = 6971;
    private static final float FREQ_XZ = 0.009F, FREQ_Y = 0.075F;

    private final int seed;

    public GTCellNoise(long worldSeed) {{
        this.seed = (int) worldSeed;
    }}

    /** A number in [0, optionCount). */
    public int get(float x, float y, float z, int optionCount) {{
        return Math.min(optionCount - 1, (int) (((get(x, y, z) + 1) / 2.0F) * optionCount));
    }}

    /** A number in [-1, 1]. */
    public float get(float x, float y, float z) {{
        x *= FREQ_XZ;
        y *= FREQ_Y;
        z *= FREQ_XZ;
        int xr = Math.round(x), yr = Math.round(y), zr = Math.round(z), xc = 0, yc = 0, zc = 0;
        float distance = Float.MAX_VALUE;
        for (int xi = xr - 1; xi <= xr + 1; xi++) {{
            for (int yi = yr - 1; yi <= yr + 1; yi++) {{
                for (int zi = zr - 1; zi <= zr + 1; zi++) {{
                    int c = (hash(seed, xi, yi, zi) & 255) * 3;
                    float vx = xi - x + CELL[c];
                    float vy = yi - y + CELL[c + 1];
                    float vz = zi - z + CELL[c + 2];
                    float d = vx * vx + vy * vy + vz * vz;
                    if (d < distance) {{
                        distance = d;
                        xc = xi; yc = yi; zc = zi;
                    }}
                }}
            }}
        }}
        int n = (X_PRIME * xc) ^ (Y_PRIME * yc) ^ (Z_PRIME * zc);
        return (n * n * n * 60493) / (float) 2147483648.0;
    }}

    private static int hash(int seed, int x, int y, int z) {{
        int hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;
        hash ^= Z_PRIME * z;
        hash = hash * hash * hash * 60493;
        return (hash >> 13) ^ hash;
    }}

    /** 256 cell offsets (x,y,z triplets) from the GT6 noise table. */
    private static final float[] CELL = {{
{table}
    }};
}}
"""
    with open(os.path.join(OUT, "GTCellNoise.java"), "w", encoding="utf-8") as f:
        f.write(content)


def write_layers(layers, contacts) -> None:
    def ore_java(o):
        mat, ind, div, lo, hi = o
        return f'ore("{mat}", {str(ind).lower()}, {div}, {lo}, {hi})'

    layer_lines = []
    for stone, block_id, material, no_deep, ores in layers:
        stone_arg = f'"{stone}"' if stone else "null"
        block_arg = f'"{block_id}"' if block_id else "null"
        nd = "true" if no_deep else "false"
        ores_java = ", ".join(ore_java(o) for o in ores)
        layer_lines.append(
            f'        layer({stone_arg}, {block_arg}, "{material}", {nd}{", " + ores_java if ores_java else ""});')

    contact_lines = []
    for both, a, b, ores in contacts:
        ores_java = ", ".join(ore_java(o) for o in ores)
        contact_lines.append(f'        contact({str(both).lower()}, "{a}", "{b}", {ores_java});')

    content = f"""package com.gregtech.gregtech.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 stone layer + contact ore tables from {{@code Loader_Worldgen}} (the source of
 * the worldgenerationnew.cfg stonelayers/doublelayers sections). Layer order matters:
 * {{@link GTCellNoise}} indexes into {{@link #LAYERS}}.
 *
 * <p>GENERATED by tools/transpile_gt6_stonelayers.py — do not edit by hand.</p>
 */
public final class GTStoneLayersGen {{
    /** Per-block ore roll inside a layer: probability = 1/chanceDiv, within [minY,maxY] (old coords). */
    public record OreDef(String material, boolean indicators, int chanceDiv, int minY, int maxY) {{}}

    /**
     * stoneType = StoneType enum name, or null. blockId = registry id of a whole-block
     * ore seam (GT6 BlockRockOres), or null. Both null = vanilla stone layer.
     * noDeep layers keep the host below the deep threshold (GT6 turned them to slate).
     */
    public record LayerDef(String stoneType, String blockId, String material, boolean noDeep, List<OreDef> ores) {{}}

    public record ContactDef(boolean bothSides, String materialA, String materialB, List<OreDef> ores) {{}}

    public static final List<LayerDef> LAYERS = new ArrayList<>();
    public static final List<ContactDef> CONTACTS = new ArrayList<>();

    private GTStoneLayersGen() {{}}

    private static OreDef ore(String material, boolean indicators, int chanceDiv, int minY, int maxY) {{
        return new OreDef(material, indicators, chanceDiv, minY, maxY);
    }}

    private static void layer(String stoneType, String blockId, String material, boolean noDeep, OreDef... ores) {{
        LAYERS.add(new LayerDef(stoneType, blockId, material, noDeep, List.of(ores)));
    }}

    private static void contact(boolean bothSides, String a, String b, OreDef... ores) {{
        CONTACTS.add(new ContactDef(bothSides, a, b, List.of(ores)));
    }}

    static {{
{chr(10).join(layer_lines)}

{chr(10).join(contact_lines)}
    }}
}}
"""
    with open(os.path.join(OUT, "GTStoneLayersGen.java"), "w", encoding="utf-8") as f:
        f.write(content)


if __name__ == "__main__":
    main()
