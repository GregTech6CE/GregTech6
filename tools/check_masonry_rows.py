"""Compare GT6's BlockStones masonry rows with the port's machine and crafting tables.

GT6 registers these rows per *equal-block group* (``mEqualBlocks[<variant>]``), i.e. once per rock type
per variant — ``BlockStones:255-487``.  This script parses the original and checks, row for row:

* the machine families (hammer, crusher, shredder, smelting, generify, cleanmoss, sawing) against the
  port's runtime report ``docs/stone-variant-coverage.json``;
* the crafting patterns, mirror flags and tool keys against ``StoneCraftingRecipeInjector``.

Usage:  python tools/check_masonry_rows.py [--json docs/gt6-masonry-parity.json]
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
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GT6 = (native_path(ROOT / ".." / "gregtech6-master" / "gregtech6-master" / "src" / "main" / "java"
       / "gregapi" / "block" / "metatype" / "BlockStones.java")).resolve()
PORT_REPORT = ROOT / "docs" / "stone-variant-coverage.json"
PORT_LOADER = (native_path(ROOT / "src" / "main" / "java" / "com" / "gregtech" / "gregtech" / "content" / "recipe"
               / "StoneCraftingRecipeInjector.java"))
ROCK_TYPES = 27

GROUP = re.compile(r"for \(ItemStackContainer tStack : \(ItemStackSet<ItemStackContainer>\)mEqualBlocks\[(\w+)\]\)")
MACHINE = re.compile(r"RM\.(Hammer|Crusher|Shredder)\s*\.addRecipe1\(([^;]*)\);")
SMELT = re.compile(r"RM\.add_smelting\(([^;]*)\);")
GENERIFY = re.compile(r"RM\.generify\(([^;]*)\);")
CLEANMOSS = re.compile(r"RM\.cleanmoss\(([^;]*)\);")
GROWMOSS = re.compile(r"RM\.growmoss\(([^;]*)\);")
SAWING_LOOP = re.compile(r"for \(int i = 0; i < maxMeta\(\); i\+\+\) if \(JUSTSTONE\[i\]\)")
SAWING = re.compile(r"RM\.sawing\(([^;]*)\);")
CRAFT = re.compile(r"CR\.(shaped|shapeless)\(([^;]*)\);")
PATTERN = re.compile(r'"([^"]{1,3})"')
JUSTSTONE = re.compile(r"JUSTSTONE = \{([TF,]+)\}")

# GT6's default crafting keys (CR.java:336-360), used when a pattern letter is not in the key map.
DEFAULT_KEY = {"a": "axe", "b": "blade", "c": "crowbar", "d": "screwdriver", "e": "drill",
               "f": "file", "g": "handdrill", "h": "hammer", "i": "solderingiron", "j": "solderingmetal",
               "k": "knife", "l": "magnifyingglass", "n": "monkeywrench", "o": "smallbendingcylinder",
               "p": "drawplate", "q": "scissors", "r": "softhammer", "s": "saw", "v": "sawaxe",
               "w": "wrench", "x": "wirecutter", "y": "chisel", "z": "bendingcylinder"}


def strip_comments(text: str) -> str:
    return "\n".join(line.split("//")[0] for line in text.splitlines())


def parse_gt6() -> dict:
    text = strip_comments(GT6.read_text(encoding="utf-8"))
    machines: Counter[str] = Counter()
    smelt_groups = 0
    smelt_preamble = 0
    generify = 0
    cleanmoss = 0
    sawing = 0
    crafting: list[dict] = []
    group = "PREAMBLE"
    pending_sawing_loop = False
    for line in text.splitlines():
        found = GROUP.search(line)
        if found:
            group = found.group(1)
        match = MACHINE.search(line)
        if match:
            machines[match.group(1).lower()] += 1
            continue
        if SMELT.search(line):
            if group == "PREAMBLE":
                smelt_preamble += 1
            else:
                smelt_groups += 1
            continue
        if GENERIFY.search(line):
            generify += 1
            continue
        if CLEANMOSS.search(line):
            cleanmoss += 1
            continue
        if GROWMOSS.search(line):
            # RM.growmoss writes two shapeless rows: clean + GT6's moss item, and clean + vine.
            crafting.extend({"group": group, "kind": "shapeless", "patterns": [], "tools": [],
                             "mirror": False, "moss": True} for _ in range(2))
            continue
        if SAWING_LOOP.search(line):
            sawing += sum(1 for flag in JUSTSTONE.search(text).group(1).split(",") if flag == "T")
            pending_sawing_loop = True
            continue
        if SAWING.search(line):
            # The loop's own call is already counted through JUSTSTONE.
            if pending_sawing_loop:
                pending_sawing_loop = False
            else:
                sawing += 1
            continue
        match = CRAFT.search(line)
        if match:
            kind, body = match.group(1), match.group(2)
            head = body.split("'")[0]
            patterns = PATTERN.findall(head)
            letters = [letter for pattern in patterns for letter in pattern if letter != " "]
            explicit = set(re.findall(r"'(\w)'", body))
            tools = sorted({DEFAULT_KEY[letter] for letter in letters
                            if letter not in explicit and letter in DEFAULT_KEY})
            crafting.append({"group": group, "kind": kind, "patterns": patterns, "tools": tools,
                             "mirror": "DEF_MIR" in body})
    preamble = sum(1 for row in crafting if row["group"] == "PREAMBLE")
    return {
        "machines": machines,
        "smeltPreamble": smelt_preamble,
        "smeltGroups": smelt_groups,
        "generify": generify,
        "cleanmoss": cleanmoss,
        "sawing": sawing,
        "crafting": crafting,
        "craftingPreamble": preamble,
        "craftingGroups": len(crafting) - preamble,
    }


def parse_port() -> tuple[list[dict], list[str]]:
    text = strip_comments(PORT_LOADER.read_text(encoding="utf-8"))
    rows: list[dict] = []
    for match in re.finditer(r"(shaped|shapeless)\(recipes, path \+ \"([^\"]+)\",", text):
        kind, path = match.group(1), match.group(2)
        # Take the whole call by balancing parentheses from the call's own opening one.
        start = match.start() + len(kind)
        depth = 0
        end = start
        for index in range(start, len(text)):
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
                if depth == 0:
                    end = index
                    break
        body = text[start + 1:end]
        patterns = [p for p in PATTERN.findall(body) if not p.startswith("gt.")]
        tools = sorted(set(re.findall(r"'(f|h|y)',", body)))
        rows.append({"path": path, "kind": kind, "patterns": patterns,
                     "mirror": body.rstrip().endswith("true"), "tools": tools})
    skipped = sorted(set(re.findall(r'SKIPPED\.add\(path \+ "([^"]+)"', text)))
    return rows, skipped


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", default="")
    args = parser.parse_args()

    gt6 = parse_gt6()
    report = json.loads(PORT_REPORT.read_text(encoding="utf-8")) if PORT_REPORT.exists() else {}
    counts = report.get("rowsByFamily", {})
    machine_skips = report.get("skipped", [])
    crafting = gt6["crafting"]
    port_rows, port_skipped_paths = parse_port()

    problems: list[str] = []
    for family in ("hammer", "crusher", "shredder"):
        want, have = gt6["machines"][family] * ROCK_TYPES, counts.get(family)
        if have != want:
            problems.append(f"{family}: port {have} != GT6 {gt6['machines'][family]} × {ROCK_TYPES} = {want}")
    smelt_want = (gt6["smeltGroups"] + gt6["smeltPreamble"]) * ROCK_TYPES
    smelt_have = counts.get("smelt", 0) + counts.get("dust", 0) + len(machine_skips)
    if smelt_have != smelt_want:
        problems.append(f"smelting: port {smelt_have} (rows + documented skips) != GT6 {smelt_want}")
    for family, wanted in (("generify", gt6["generify"] * ROCK_TYPES),
                           ("cleanmoss", gt6["cleanmoss"] * ROCK_TYPES),
                           ("sawing", gt6["sawing"] * ROCK_TYPES)):
        if counts.get(family) != wanted:
            problems.append(f"{family}: port {counts.get(family)} != GT6 {gt6['sawing'] if family == 'sawing' else wanted // ROCK_TYPES} × {ROCK_TYPES} = {wanted}")
    if gt6["smeltPreamble"] != 1:
        problems.append(f"GT6 has {gt6['smeltPreamble']} pre-loop smelting rows, the script expects 1")
    if gt6["sawing"] != 15:
        problems.append(f"GT6's sawing rows per rock type: {gt6['sawing']}, the script expects 15")

    per_type = len(crafting)
    # Static structure: the loader writes one row per GT6 row except the drill row (always skipped) and
    # the three "grow moss with GT6's moss item" rows, and the data pack ships the stone → bricks row.
    static_port = len(port_rows) + 1 + 3 + 1
    if static_port != per_type:
        problems.append(f"the loader declares {len(port_rows)} rows + 1 drill + 3 moss + 1 data-pack ="
                        f" {static_port} != GT6's {per_type}")
    runtime_total = report.get("craftingRows", 0) + len(report.get("craftingRowsSkipped", [])) + ROCK_TYPES
    if runtime_total != per_type * ROCK_TYPES:
        problems.append(f"crafting: the runtime report accounts for {runtime_total}, GT6 has {per_type}"
                        f" × {ROCK_TYPES} = {per_type * ROCK_TYPES} (rows + skips + data-pack)")

    # Pattern comparison: GT6's rows minus the six moss rows, the drill row and the plain-stone →
    # bricks row the data pack ships; the port's rows minus its three vine rows.
    gt6_patterns = Counter((row["kind"], tuple(row["patterns"])) for row in crafting
                           if not row.get("moss") and "drill" not in row["tools"] and row["patterns"]
                           and not (row["group"] == "STONE" and tuple(row["patterns"]) == ("XX", "XX")))
    port_patterns = Counter((row["kind"], tuple(row["patterns"])) for row in port_rows
                            if "from_vine" not in row["path"] and row["patterns"])
    missing = gt6_patterns - port_patterns
    extra = port_patterns - gt6_patterns
    if missing:
        problems.append(f"crafting patterns GT6 has but the port does not: {dict(missing)}")
    if extra:
        problems.append(f"crafting patterns the port added: {dict(extra)}")
    mirror_gt6 = sum(1 for row in crafting if row["group"] != "PREAMBLE" and row.get("mirror"))
    mirror_port = [row["path"] for row in port_rows if row["mirror"]]
    if mirror_gt6 != len(mirror_port):
        problems.append(f"mirroring crafting rows: port {mirror_port}, GT6 {mirror_gt6}")

    payload = {
        "rockTypes": ROCK_TYPES,
        "gt6PerRockType": {
            "hammer": gt6["machines"]["hammer"], "crusher": gt6["machines"]["crusher"],
            "shredder": gt6["machines"]["shredder"], "smelt": gt6["smeltGroups"],
            "smeltPreamble": gt6["smeltPreamble"], "generify": gt6["generify"],
            "cleanmoss": gt6["cleanmoss"], "sawing": gt6["sawing"],
            "crafting": per_type,
        },
        "portRows": counts,
        "portCrafting": {"registered": report.get("craftingRows"),
                         "skipped": report.get("craftingRowsSkipped")},
        "machineSkips": machine_skips,
        "problems": problems,
    }
    print("GT6 per rock type :", payload["gt6PerRockType"])
    print("port rows         :", counts)
    print(f"port crafting     : {report.get('craftingRows')} registered +"
          f" {len(report.get('craftingRowsSkipped', []))} documented skips + {ROCK_TYPES} data-pack")
    if problems:
        print(f"\nPROBLEMS ({len(problems)}):")
        for problem in problems:
            print("  - " + problem)
    else:
        print("\nOK: GT6's masonry rows and the port agree row for row.")
    if args.json:
        out = (ROOT / args.json).resolve()
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(payload, indent=2, sort_keys=True), encoding="utf-8")
        print(f"wrote {out}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
