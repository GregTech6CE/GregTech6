#!/usr/bin/env python3
"""Generate the centrifuge rows for GT6's combs (`MultiItemFood.java:251-270`).

GT6 registers its combs inside `MultiItemFood` and processes them with 20
`RM.Centrifuge.addRecipe1(...)` calls — a file the transpiler never scans, so the port had the comb
items (from `transpile_gt6_multiitems.py`) but no way to turn them into anything.

Every GT6 expression these rows use is mapped explicitly below; an expression the port cannot
express is recorded as a comment instead of being guessed at.

Inputs : gregtech6-master .../gregtech/items/MultiItemFood.java
Output : src/main/java/com/gregtech/gregtech/content/recipe/generated/GTCombGen.java
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os
import re

GT6 = (r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregtech\items"
       r"\MultiItemFood.java")
TARGET = os.path.join(str(_NativePath(__file__).resolve().parents[1] / "core" / "src"), "main", "java", "com", "gregtech", "gregtech", "content", "recipe", "generated",
                      "GTCombGen.java")

# GT6 comb field -> port item id (the multi-item table's snake_case of the display name).
COMBS = {
    "Comb_Honey": "honey_comb", "Comb_Water": "water_comb", "Comb_Magic": "magic_comb",
    "Comb_Nether": "nether_comb", "Comb_End": "end_comb", "Comb_Rock": "rock_comb",
    "Comb_Jungle": "jungle_comb", "Comb_Frozen": "frozen_comb", "Comb_Shroom": "shroomy_comb",
    "Comb_Sandy": "sandy_comb", "Comb_Clay": "clay_comb", "Comb_Sticky": "sticky_comb",
    "Comb_Royal": "royal_comb", "Comb_Soul": "soul_comb", "Comb_Amnesic": "amnesic_comb",
    "Comb_Military": "military_comb", "Comb_Pyro": "pyro_comb", "Comb_Cryo": "cryo_comb",
    "Comb_Aero": "aero_comb", "Comb_Tera": "tera_comb",
}

# GT6 fluid expression -> port spec. `L` is GT6's 144 mB unit, `U` a whole material unit.
FLUIDS = {
    "FL.Honey": "Honey", "FL.Water": "Water", "FL.Ambrosia": "Ambrosia", "FL.Blaze": "Blaze",
    "FL.Dragon_Breath": "Dragon_Breath", "FL.Concrete": "Concrete", "FL.Latex": "Latex",
    "FL.Ice": "Ice", "FL.Oil_Soulsand": "Oil_Soulsand", "FL.lube": "Lubricant",
    "FL.Potion_Harm_1": "Potion_Harm_1", "FL.Soup_Mushroom": "Soup_Mushroom",
    "FL.Juice_Cactus": "Juice_Cactus", "FL.RoyalJelly": "RoyalJelly",
}

# GT6 vanilla item expressions and the mod items whose 1.20.1 stand-in is the vanilla item
# (`IL.EtFu_Chorus_Fruit` is Et Futurum's backport of the chorus fruit).
VANILLA = {
    "Items.string": ("string", 1), "Items.sand": ("sand", 1), "Items.bone": ("bone", 1),
    "Items.rotten_flesh": ("rotten_flesh", 1), "Items.spider_eye": ("spider_eye", 1),
    "Blocks.sand": ("sand", 1), "Blocks.red_mushroom_block": ("red_mushroom_block", 1),
    "Blocks.brown_mushroom_block": ("brown_mushroom_block", 1),
}

# Mod items that exist in the port under its own id (GT6's `IL.X` technological items).
MOD_ITEMS = {
    "IL.EtFu_Chorus_Fruit": "v:chorus_fruit:1",
    "IL.Resin": "tech:resin:1",
}


def fluid_amount(arg: str) -> int:
    """GT6's amount expressions in these rows: a literal, `L` (144) or `L/2`."""
    arg = arg.strip().rstrip(",)").strip()
    if arg == "L":
        return 144
    if arg == "L/2":
        return 72
    m = re.fullmatch(r"(\d+)", arg)
    return int(m.group(1)) if m else 0


def fluid_specs(expr: str) -> list:
    """One or more fluid specs: GT6's `FL.array(a, b)` yields several outputs."""
    expr = expr.strip()
    if expr.startswith("FL.array(") or expr.startswith("FL .array("):
        inner = expr[expr.index("(") + 1:expr.rindex(")")]
        out = []
        for part in split_args(inner):
            spec = fluid_spec(part)
            if not spec:
                return []
            out.append(spec)
        return out
    spec = fluid_spec(expr)
    return [spec] if spec else []


def fluid_spec(expr: str) -> str:
    """`FL.Honey.make(100)` -> `f:Honey:100`, `MT.Chocolate.liquid(U, T)` -> `m:Chocolate:144`."""
    expr = expr.strip()
    m = re.match(r"(FL\.\w+)\s*\.make\(([^)]*)\)", expr)
    if m:
        name = FLUIDS.get(m.group(1))
        if name is None:
            return ""
        amount = fluid_amount(m.group(2))
        if m.group(1) == "FL.Water":
            return "w:%d" % amount
        return "f:%s:%d" % (name, amount)
    # GT6's FL.lube(n) helper is the lubricant fluid.
    m = re.match(r"FL\s*\.lube\(([^)]*)\)", expr)
    if m:
        return "f:Lubricant:%d" % fluid_amount(m.group(1))
    m = re.match(r"MT\.(\w+)\s*\.liquid\(\s*U\s*,", expr)
    if m:
        return "m:%s:144" % m.group(1)
    return ""


def item_spec(expr: str) -> str:
    """`OM.dust(MT.WaxBee)` -> `i:dust:WaxBee:1`, `Items.string` -> `v:string:1`."""
    expr = expr.strip()
    # GT6's `IL.X.get(1, fallback)` — the mod's item, or the fallback when the mod is absent. This
    # port has no Forestry/IC2, so the fallback is the faithful choice.
    m = re.match(r"IL\.\w+\s*\.get\(\s*\d+\s*,\s*(.+)\)$", expr)
    if m:
        return item_spec(m.group(1))
    if expr in MOD_ITEMS:
        return MOD_ITEMS[expr]
    m = re.match(r"OM\s*\.dust\(\s*MT\.(\w+)\s*,\s*U9\s*\)", expr)
    if m:
        return "i:dustTiny:%s:9" % m.group(1)
    m = re.match(r"OM\s*\.dust\(\s*MT\.(\w+)\s*\)", expr)
    if m:
        return "i:dust:%s:1" % m.group(1)
    m = re.match(r"OP\s*\.stick\s*\.mat\(\s*MT\.(\w+)\s*,\s*(\d+)\s*\)", expr)
    if m:
        return "i:stick:%s:%s" % (m.group(1), m.group(2))
    m = re.match(r"ST\s*\.make\((\w+\.\w+)\s*,\s*(\d+)", expr)
    if m and m.group(1) in VANILLA:
        name, _ = VANILLA[m.group(1)]
        return "v:%s:%s" % (name, m.group(2))
    return ""


def split_args(text: str) -> list:
    """Split a GT6 argument list on top-level commas, keeping `(...)` and `{...}` together."""
    out, depth, current = [], 0, ""
    for char in text:
        if char in "({":
            depth += 1
        elif char in ")}":
            depth -= 1
        if char == "," and depth == 0:
            out.append(current.strip())
            current = ""
        else:
            current += char
    if current.strip():
        out.append(current.strip())
    return out


def main() -> int:
    text = io.open(GT6, encoding="utf-8", errors="replace").read()
    rows, skipped = [], []
    for line in text.splitlines():
        line = line.split("//")[0]
        if "RM.Centrifuge.addRecipe1" not in line:
            continue
        args = split_args(line[line.index("(") + 1:line.rindex(")")])
        # (optimize, eut, duration, chances, itemIn, fluidIn, fluidOut, itemOut...)
        eut, duration = int(args[1]), int(args[2])
        chances = [int(c) for c in re.findall(r"\d+", args[3])]
        comb = re.match(r"IL\.(\w+)\s*\.get", args[4])
        if not comb or comb.group(1) not in COMBS:
            skipped.append((line.strip(), "unknown comb input"))
            continue
        comb_id = COMBS[comb.group(1)]
        fluid_in = "" if args[5].strip() in ("NF", "ZL_FS") else fluid_spec(args[5])
        if args[5].strip() not in ("NF", "ZL_FS") and not fluid_in:
            skipped.append((line.strip(), "unmapped input fluid " + args[5]))
            continue
        fluid_out, item_out, dropped = [], [], []
        for arg in args[6:]:
            arg = arg.strip()
            if arg in ("NF", "ZL_FS", "NONE"):
                continue
            specs = fluid_specs(arg)
            if specs:
                fluid_out.extend(specs)
                continue
            spec = item_spec(arg)
            if spec:
                item_out.append(spec)
                continue
            # GT6's rows mix other mods' items (Forestry propolis, Et Futurum, IC2) into otherwise
            # portable rows: keep the row, drop that one output and say so in the table.
            dropped.append(arg)
        rows.append((comb_id, eut, duration, chances, fluid_in, fluid_out, item_out, dropped))

    print("comb centrifuge rows: %d (skipped %d)" % (len(rows), len(skipped)))
    for line, why in skipped:
        print("   skip (%s): %s" % (why, line[:110].encode("ascii", "replace").decode()))

    lines = [
        "package com.gregtech.gregtech.content.recipe.generated;",
        "",
        "/**",
        " * GT6's comb processing: the twenty {@code RM.Centrifuge.addRecipe1} rows of",
        " * {@code MultiItemFood.java:251-270}, which turn a comb into its fluid, wax and side products.",
        " *",
        " * <p>GT6 registers those rows inside its food item class, which",
        " * {@code tools/transpile_gt6_chem.py} never scans, so the port had the comb items",
        " * ({@code transpile_gt6_multiitems.py}) but nothing to do with them.</p>",
        " *",
        " * <p>GENERATED by tools/extract_gt6_comb_recipes.py - do not edit by hand.",
        " * Spec resolution happens in {@link GTGeneratedChem}.</p>",
        " */",
        "final class GTCombGen {",
        "    private static final String[] NONE = new String[0];",
        "",
        "    private GTCombGen() {}",
        "",
        "    private static String[] sp(String... specs) { return specs; }",
        "",
        "    private static long[] ch(long... chances) { return chances; }",
        "",
        "    private static void r(long eut, long dur, long[] chances, String[] itemIn,",
        "                          String[] fluidIn, String[] fluidOut, String[] itemOut) {",
        '        GeneratedRecipeSink.register("Centrifuge", eut, dur, chances, itemIn, fluidIn, fluidOut, itemOut);',
        "    }",
        "",
        "    static void load() {",
    ]
    for comb_id, eut, duration, chances, fluid_in, fluid_out, item_out, dropped in rows:
        if dropped:
            lines.append("        // GT6 also yields %s (another mod's item); dropped here."
                         % ", ".join(dropped))
        lines.append(
            '        r(%d, %d, ch(%s), sp("tech:%s:1"), %s, %s, %s);'
            % (eut, duration, ", ".join(str(c) for c in chances), comb_id,
               ('sp("%s")' % fluid_in) if fluid_in else "NONE",
               ("sp(%s)" % ", ".join('"%s"' % f for f in fluid_out)) if fluid_out else "NONE",
               ("sp(%s)" % ", ".join('"%s"' % i for i in item_out)) if item_out else "NONE"))
    lines += [
        "    }",
        "}",
        "",
    ]
    io.open(TARGET, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    print("wrote %s (%d rows)" % (TARGET.replace("\\", "/"), len(rows)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
