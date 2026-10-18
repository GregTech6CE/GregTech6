"""Compare the GT6 BlockStones extruder rows with the port's stone extrusion table.

GT6 adds the stone/cobblestone extrusion rows in
``gregapi/block/metatype/BlockStones.java`` inside the ``mEqualBlocks[STONE]`` and
``mEqualBlocks[COBBL]`` loops.  Each row is

    RM.Extruder.addRecipe2(F, F, F, F, T, 16, 32, ST.amount(1, tStack.toStack()),
                           IL.Shape_Extruder_Plate.get(0), OP.plate.mat(mMaterial, 9));

i.e. five explicit flags (optimize, checkForCollisions, fake, hidden, logErrors), the EU/t, the
duration, the block as input, the mould and the output.  This script extracts all of them, maps the
GT6 names to the port's ids and diffs the two tables, so a dropped or extra row cannot slip in.

Usage:  python tools/check_stone_extrusions.py [--json docs/gt6-stone-extrusion-parity.json]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GT6_BLOCKSTONES = (native_path(ROOT / ".." / "gregtech6-master" / "gregtech6-master" / "src" / "main" / "java"
                   / "gregapi" / "block" / "metatype" / "BlockStones.java")).resolve()
PORT_RECIPES = native_path(ROOT / "src" / "main" / "java" / "com" / "gregtech" / "gregtech" / "content" / "recipe" / "StoneAndToolSurvivalRecipes.java")

# GT6 IL.Shape_* name -> port mould id suffix.
MOULD = {
    "Plate": "plate",
    "Plate_Curved": "curvedplate",
    "Rod": "rod",
    "Rod_Long": "longrod",
    "Bolt": "bolt",
    "Ingot": "ingot",
    "Block": "block",
    "Shovel": "shovelhead",
    "Sword": "swordblade",
    "Hoe": "hoehead",
    "Pickaxe": "pickaxehead",
    "Axe": "axehead",
    "Gear": "gear",
    "Gear_Small": "smallgear",
    "Hammer": "hammerhead",
}

MOULD_FAMILY = {
    "Shape_Extruder_": "extruder_shape_",
    "Shape_SimpleEx_": "low_heat_extruder_shape_",
}

# GT6 OP.<name> -> port item prefix used in GTGeneratedChem specs.
OP_PREFIX = {
    "plate": "plate",
    "plateCurved": "plateCurved",
    "stick": "stick",
    "stickLong": "stickLong",
    "bolt": "bolt",
    "toolHeadRawShovel": "toolHeadRawShovel",
    "toolHeadRawSword": "toolHeadRawSword",
    "toolHeadRawHoe": "toolHeadRawHoe",
    "toolHeadRawPickaxe": "toolHeadRawPickaxe",
    "toolHeadRawAxe": "toolHeadRawAxe",
    "gearGt": "gearGt",
    "gearGtSmall": "gearGtSmall",
    "toolHeadHammer": "toolHeadHammer",
}

GROUP_RE = re.compile(r"for \(ItemStackContainer tStack : \(ItemStackSet<ItemStackContainer>\)mEqualBlocks\[(\w+)\]\) \{")
ROW_RE = re.compile(
    r"RM\.Extruder\.addRecipe2\(([^;]*?)\);",
    re.S,
)


def strip_comments(text: str) -> str:
    out = []
    for line in text.splitlines():
        idx = line.find("//")
        out.append(line if idx < 0 else line[:idx])
    return "\n".join(out)


def parse_gt6() -> list[dict]:
    text = strip_comments(GT6_BLOCKSTONES.read_text(encoding="utf-8"))
    rows: list[dict] = []
    group = None
    for line in text.splitlines():
        found = GROUP_RE.search(line)
        if found:
            group = found.group(1)
        if "RM.Extruder.addRecipe2" not in line:
            continue
        body = line[line.index("RM.Extruder.addRecipe2(") + len("RM.Extruder.addRecipe2("):].rstrip()
        if not body.endswith(");"):
            raise SystemExit(f"cannot parse GT6 extruder row: {line.strip()}")
        body = body[:-2]
        args = [a.strip() for a in body.split(",")]
        flags = args[0:5]
        eut, duration = args[5], args[6]
        # The input and the output contain commas inside method calls, so re-split on the known shape.
        rest = ",".join(args[7:])
        match = re.match(r"ST\.amount\(\s*1\s*,\s*tStack\.toStack\(\)\s*\)\s*,\s*"
                         r"IL\.(Shape_\w+?)\s*\.get\(0\)\s*,\s*(.+)$", rest)
        if not match:
            raise SystemExit(f"cannot parse GT6 extruder row: {line.strip()}")
        shape, output = match.group(1), match.group(2).strip().rstrip(";")
        family, name = next((f, shape[len(f):]) for f in MOULD_FAMILY if shape.startswith(f))
        if name not in MOULD:
            raise SystemExit(f"unknown GT6 shape {shape}")
        block = re.match(r"ST\.make\(\s*this\s*,\s*1\s*,\s*(\w+)\s*\)$", output)
        if block:
            out_kind, out_amount = "block:" + block.group(1), 1
        else:
            op = re.match(r"OP\.(\w+)\.mat\(\s*mMaterial\s*,\s*(\d+)\s*\)$", output)
            if not op:
                raise SystemExit(f"unknown GT6 output {output}")
            if op.group(1) not in OP_PREFIX:
                raise SystemExit(f"unknown GT6 output prefix {op.group(1)}")
            out_kind, out_amount = "item:" + OP_PREFIX[op.group(1)], int(op.group(2))
        rows.append({
            "group": group,
            "optimize": flags[0],
            "checkForCollisions": flags[1],
            "fake": flags[2],
            "hidden": flags[3],
            "logErrors": flags[4],
            "eut": int(eut),
            "duration": int(duration),
            "mould": MOULD_FAMILY[family] + MOULD[name],
            "output": out_kind,
            "amount": out_amount,
        })
    return rows


def parse_port() -> dict:
    text = strip_comments(PORT_RECIPES.read_text(encoding="utf-8"))
    shapes = {}
    for name, spec in re.findall(r'\{"(\w+)",\s*"(i:[\w:%.]+)"\}', text):
        shapes[name] = spec
    families = re.findall(r'STONE_EXTRUDER_MOLDS\s*=\s*\{([^}]*)\}', text)
    moulds = re.findall(r'"([\w_]*extruder_shape_)"', families[0]) if families else []
    call = re.search(r"addRecipe2\(([^)]*?), 16, 32, input, mold, output\)", text)
    if not call:
        raise SystemExit("cannot find the port's extrude() call")
    flags = [f.strip() for f in call.group(1).split(",")]
    block_rows = re.findall(r'extrude\(input, mold\(family \+ "(\w+)"\), (\w+)\)', text)
    return {
        "shapes": shapes,
        "moulds": moulds,
        "flags": flags,
        "blockRows": block_rows,
    }


BLOCK_VARIABLE = {"bricks": "BRICK", "plain": "STONE"}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", default="")
    args = parser.parse_args()

    gt6 = parse_gt6()
    port = parse_port()

    problems: list[str] = []
    port_rows = (len(port["shapes"]) + len(port["blockRows"])) * len(port["moulds"])
    groups = sorted({r["group"] for r in gt6})
    for group in groups:
        rows = [r for r in gt6 if r["group"] == group]
        if len(rows) != port_rows:
            problems.append(f"{group}: GT6 has {len(rows)} rows, the port has {port_rows}")
        for family in port["moulds"]:
            expected = {family + name for name in port["shapes"]}
            expected |= {family + row[0] for row in port["blockRows"]}
            got = {r["mould"] for r in rows if r["mould"].startswith(family)}
            missing, extra = expected - got, got - expected
            if missing or extra:
                problems.append(f"{group}/{family}: missing {sorted(missing)} extra {sorted(extra)}")
        for row in rows:
            if (row["optimize"], row["checkForCollisions"]) != ("F", "F"):
                problems.append(f"{group}: {row['mould']} is registered with "
                                f"optimize={row['optimize']} checkForCollisions={row['checkForCollisions']}")
            if (row["eut"], row["duration"]) != (16, 32):
                problems.append(f"{group}: {row['mould']} runs at {row['eut']} EU/t for {row['duration']}")
            for family in port["moulds"]:
                if not row["mould"].startswith(family):
                    continue
                suffix = row["mould"][len(family):]
                if row["output"].startswith("item:"):
                    spec = port["shapes"].get(suffix)
                    if spec != f"i:{row['output'][5:]}:%s:{row['amount']}":
                        problems.append(f"{group}: {row['mould']} GT6 wants "
                                        f"{row['output'][5:]} x{row['amount']}, port has {spec}")
                else:
                    variant = row["output"][6:]
                    found = [row2 for row2 in port["blockRows"] if row2[0] == suffix]
                    if not found:
                        problems.append(f"{group}: {row['mould']} GT6 wants block {variant}, port has no row")
                    elif BLOCK_VARIABLE.get(found[0][1], found[0][1]) != variant:
                        problems.append(f"{group}: {row['mould']} GT6 wants block {variant}, "
                                        f"port uses {found[0][1]}")

    if port["flags"] != ["false", "false", "false", "false", "true"]:
        problems.append(f"port extrude() flags are {port['flags']}, GT6 uses F, F, F, F, T")
    if len(port["moulds"]) != len(MOULD_FAMILY):
        problems.append(f"port mould families: {port['moulds']}")

    report = {
        "gt6Rows": len(gt6),
        "gt6Groups": groups,
        "portShapes": sorted(port["shapes"]),
        "portMouldFamilies": port["moulds"],
        "portFlags": port["flags"],
        "problems": problems,
    }
    print(f"GT6 BlockStones extruder rows : {len(gt6)} in groups {groups}")
    print(f"port shapes                   : {len(port['shapes'])} x {len(port['moulds'])} mould families "
          f"+ {len(port['blockRows'])} block rows")
    print(f"port extrude() flags          : {', '.join(port['flags'])}")
    if problems:
        print(f"\nPROBLEMS ({len(problems)}):")
        for problem in problems:
            print("  - " + problem)
    else:
        print("\nOK: the port's stone extrusion table matches GT6 row for row.")
    if args.json:
        out = (ROOT / args.json).resolve()
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")
        print(f"wrote {out}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
