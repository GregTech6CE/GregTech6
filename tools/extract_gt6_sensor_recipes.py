"""Extracts GT6's 21 sensor recipes from ``Loader_MultiTileEntities`` (§109 prep).

The port ships GT6's 21 sensor machines and their crafting recipes. The originals are the 21
``aRegistry.add(...)`` rows of ``gregtech/loaders/b/Loader_MultiTileEntities.java:1979-1999``, each of
which ends in a ``CR.shaped`` payload: a 3x3 pattern plus a ``'C', expression`` key map.

This tool parses those rows and translates every key expression into the port's own ingredient grammar
(``MachineRecipeIngredients``), so the table can be dropped next to the other generated data and
resolved by the same ``MultiblockRecipePack`` machinery the multiblock controllers already use.

Usage:
  python tools/extract_gt6_sensor_recipes.py             # print the table
  python tools/extract_gt6_sensor_recipes.py --write     # write the generated Java table
  python tools/extract_gt6_sensor_recipes.py --check     # fail when the file differs (idempotency)
"""

import argparse
import io
import os
import re
import sys

from pathlib import Path
REPO = Path(__file__).resolve().parents[1]
GT6 = str(REPO.parent / 'gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java')

# GT6's registration rows carry the machine id in the third argument; the block ids the port registers
# are the same names the loader produces ("sensor_thermometer" and friends), so the table only has to
# carry the port id it already knows plus the GT6 name for review.
PORT_IDS = {
    31000: "sensor_thermometer", 31001: "sensor_gibblometer", 31002: "sensor_luminometer",
    31003: "sensor_chronometer", 31004: "sensor_itemometer", 31005: "sensor_stackometer",
    31006: "sensor_fluidometer", 31007: "sensor_bucketometer", 31010: "sensor_weightometer_light",
    31011: "sensor_weightometer_medium", 31012: "sensor_weightometric",
    31013: "sensor_weightometer_super_heavy", 31015: "sensor_electrometer", 31016: "sensor_tpsmeter",
    31017: "sensor_playercounter", 31018: "sensor_progressmeter", 31019: "sensor_tachometer",
    31020: "sensor_geiger", 31021: "sensor_laserometer", 31022: "sensor_kilobucketometer",
    31023: "sensor_kilogibblometer",
}

# GT6 key expression -> port ingredient spec. Every entry is a deliberate translation:
#   * OP.<prefix>.dat(MT.<material>) is the material item form -> mat:<prefix>@<port material name>,
#   * OP.wireGt01.dat(X) is the 1x wire -> wire:1@X (the multiblock table spells it the same way),
#   * the oredict constants are vanilla items in 1.20.1 -> item:minecraft:<name>,
#   * IL.<instrument> are the port's own technological items -> item:gregtech:<id>,
#   * IL.SENSORS[1] is the tiered sensor component -> il:SENSORS (the resolver appends the tier).
KEY_MAP = {
    "OP.plateDouble.dat(MT.TinAlloy)": "mat:plateDouble@TinAlloy",
    "OP.wireFine.dat(MT.RedAlloy)": "mat:wireFine@RedAlloy",
    "OP.bolt.dat(MT.TinAlloy)": "mat:bolt@TinAlloy",
    "OP.plate.dat(MT.Pb)": "mat:plate@Lead",
    "OP.plateDouble.dat(MT.Pb)": "mat:plateDouble@Lead",
    "OP.plate.dat(ANY.Si)": "mat:plate@Silicon",
    "OP.plate.dat(MT.Brass)": "mat:plate@Brass",
    "OP.plate.dat(MT.SteelGalvanized)": "mat:plate@SteelGalvanized",
    "OP.screw.dat(MT.Brass)": "mat:screw@Brass",
    "OP.screw.dat(MT.SteelGalvanized)": "mat:screw@SteelGalvanized",
    "OP.gearGtSmall.dat(MT.Brass)": "mat:gearGtSmall@Brass",
    "OP.gearGt.dat(MT.Brass)": "mat:gearGt@Brass",
    "OP.wireGt01.dat(ANY.Cu)": "wire:1@Copper",
    "OP.wireFine.dat(ANY.Cu)": "mat:wireFine@Copper",
    "OP.gem.dat(ANY.SiO2)": "item:minecraft:quartz",
    "OP.gem.dat(ANY.Diamond)": "mat:gem@Diamond",
    "OD.itemRedstone": "item:minecraft:redstone",
    "OD.blockGlassColorless": "item:minecraft:glass",
    "OD.pressurePlateWood": "item:minecraft:oak_pressure_plate",
    "OD.pressurePlateStone": "item:minecraft:stone_pressure_plate",
    "OD.pressurePlateIron": "item:minecraft:heavy_weighted_pressure_plate",
    "OD.pressurePlateGold": "item:minecraft:light_weighted_pressure_plate",
    "OD.craftingChest": "item:minecraft:chest",
    "Items.comparator": "item:minecraft:comparator",
    "Items.bucket": "item:minecraft:bucket",
    "Items.clock": "item:minecraft:clock",
    "IL.Thermometer_Quicksilver": "item:gregtech:quicksilver_thermometer",
    "IL.Geiger_Counter": "item:gregtech:geiger_counter",
    "IL.Electro_Meter": "item:gregtech:electrometer",
    "IL.Tacho_Meter": "item:gregtech:tachometer",
    "IL.SENSORS[1]": "il:SENSORS",
}

ROW = re.compile(
    r'aRegistry\.add\(\s*"([^"]+)"\s*,\s*"Sensors"\s*,\s*(\d+)\s*,\s*\d+\s*,\s*[\w.]+\.class\s*,'
    r'\s*\d+\s*,\s*\d+\s*,\s*\w+\s*,\s*null\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,(.*?)\);')
# The key payload is a flat list of ``'X', expression`` pairs. Splitting on the commas that a quoted
# key follows keeps expressions such as OP.plate.dat(MT.X) and IL.SENSORS[1] in one piece.
KEY_SPLIT = re.compile(r",\s*(?='\w'\s*,)")
KEY_PAIR = re.compile(r"'(\w)'\s*,\s*(.+)")


def parse(path):
    """All 21 sensor rows: (machine id, GT6 name, pattern rows, {key: gt6 expression})."""
    text = io.open(path, encoding="utf-8", errors="replace").read()
    rows = []
    for match in ROW.finditer(text):
        name, machine_id, r0, r1, r2, keys = match.groups()
        machine_id = int(machine_id)
        if machine_id not in PORT_IDS:
            continue
        pairs = {}
        for chunk in KEY_SPLIT.split(keys):
            pair = KEY_PAIR.fullmatch(chunk.strip())
            if pair is None:
                raise SystemExit("row %s: cannot read the key chunk %r" % (name, chunk))
            pairs[pair.group(1)] = " ".join(pair.group(2).split())
        if len(pairs) < 6:
            raise SystemExit("row %s parsed only %d keys: %r" % (name, len(pairs), pairs))
        # GT6's rows declare a few keys the pattern does not use (the thermometer row carries G, B and
        # C without a G/B/C in "WRW"/"RXR"/"WPW"), so the meaningful check is the other direction:
        # every character the pattern uses must have an expression.
        for pattern_row in (r0, r1, r2):
            for character in pattern_row:
                if character not in pairs:
                    raise SystemExit("row %s: pattern %r uses %r without a key"
                                     % (name, pattern_row, character))
        rows.append((machine_id, name, [r0, r1, r2], pairs))
    rows.sort()
    return rows


def translate(rows):
    """Replaces GT6 expressions with port specs, reporting anything unmapped."""
    out = []
    unknown = set()
    for machine_id, name, pattern, pairs in rows:
        mapped = {}
        for key, expr in pairs.items():
            spec = KEY_MAP.get(expr)
            if spec is None:
                unknown.add(expr)
                spec = "UNMAPPED"
            mapped[key] = spec
        out.append((machine_id, name, pattern, mapped))
    if unknown:
        raise SystemExit("unmapped GT6 key expressions: %s" % sorted(unknown))
    return out


def java(rows):
    lines = [
        "package com.gregtech.gregtech.data.generated;",
        "",
        "import java.util.List;",
        "import java.util.Map;",
        "",
        "/**",
        " * GENERATED by tools/extract_gt6_sensor_recipes.py - do not edit by hand.",
        " *",
        " * <p>The crafting recipes of GT6's 21 sensor machines",
        " * ({@code Loader_MultiTileEntities.java:1979-1999}, one {@code aRegistry.add} row each).",
        " * The pattern and the key map are the original's; the key <em>values</em> were translated from",
        " * GT6's expressions into the port's ingredient grammar",
        " * ({@code MachineRecipeIngredients}), one deliberate mapping per entry, listed in the tool.</p>",
        " */",
        "public final class GTSensorRecipesGen {",
        "    private GTSensorRecipesGen() {}",
        "",
        "    /** GT6 machine id, GT6 name, port block id, 3x3 pattern rows, port ingredient per key. */",
        "    public record Row(int machineId, String name, String blockId, List<String> pattern,",
        "                      Map<Character, String> keys) {}",
        "",
        "    public static final List<Row> ROWS = List.of(",
    ]
    # Java has no trailing comma in a method argument list, so the rows are joined with ",\n" and the
    # last one is left bare in front of the closing ");" - emitting "row,\n);" is a syntax error
    # ("illegal start of expression" pointing at the closing paren).
    row_texts = []
    for machine_id, name, pattern, keys in rows:
        key_parts = ", ".join("Map.entry('%s', \"%s\")" % (k, keys[k]) for k in sorted(keys))
        row_texts.append("            new Row(%d, \"%s\", \"%s\", List.of(\"%s\", \"%s\", \"%s\"), "
                         "Map.ofEntries(%s))"
                         % (machine_id, name, PORT_IDS[machine_id], pattern[0], pattern[1], pattern[2],
                            key_parts))
    lines.append(",\n".join(row_texts))
    lines += ["    );", "}", ""]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="write the generated Java table")
    parser.add_argument("--check", action="store_true", help="fail when the file on disk differs")
    parser.add_argument("--source", default=GT6, help="read-only original Loader_MultiTileEntities.java")
    parser.add_argument("--out", default="core/src/main/java/com/gregtech/gregtech/data/generated/GTSensorRecipesGen.java")
    args = parser.parse_args()

    if not os.path.exists(args.source):
        raise SystemExit("GT6 source not found: %s" % args.source)
    rows = translate(parse(args.source))
    text = java(rows)

    if args.check:
        if not os.path.exists(args.out):
            print("missing %s" % args.out)
            return 1
        existing = io.open(args.out, encoding="utf-8").read()
        if existing != text:
            print("%s is out of date" % args.out)
            return 1
        print("%s is up to date (%d rows)" % (args.out, len(rows)))
        return 0

    if args.write:
        os.makedirs(os.path.dirname(args.out), exist_ok=True)
        with io.open(args.out, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
        print("wrote %s (%d rows)" % (args.out, len(rows)))
        return 0

    for machine_id, name, pattern, keys in rows:
        print("%-6d %-32s %-28s %s | %s" % (machine_id, name, PORT_IDS[machine_id], "/".join(pattern),
                                            " ".join("%s=%s" % (k, keys[k]) for k in sorted(keys))))
    print("%d rows" % len(rows))
    return 0


if __name__ == "__main__":
    sys.exit(main())
