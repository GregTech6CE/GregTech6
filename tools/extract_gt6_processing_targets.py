"""Generate GT6's seven remaining "Processing Data" targets (`MT.java` -> material table).

GT6's material dictionary prints eleven processing targets per material
(`gregapi/util/UT.java`, `Books.addMaterialDictionary`, the three `Processing Data` pages):
smelting, solidifying, burning, pulverising, crushing / bending, compressing, cutting, forging,
smashing / working. The port models four of them on {@code GTMaterial} itself
({@code setSmelting}/{@code setBurning}/{@code setPulver}/{@code setCrushing}); the other seven are
only ever set explicitly for a handful of materials, so they are extracted here into a side table
instead of widening the material API.

GT6's defaults ({@code gregapi/oredict/OreDictMaterial.java:287-294}) are the material itself with one
unit for all seven; `null` as the target means "itself" as well (`setBending` etc. substitute `this`),
and an amount of 0 disables the process ("nothing" on the page).

Inputs : gregtech6-master .../gregapi/data/MT.java
Output : core/src/main/java/com/gregtech/gregtech/data/generated/MaterialProcessingTargets.java

Usage: python tools/extract_gt6_processing_targets.py --source <MT.java> --check
This legacy extraction only records explicit setters. Copied/factory-inherited
targets remain incomplete (ALIGN-012); --check proves reproducibility, not parity.
"""

from __future__ import annotations

import argparse
from pathlib import Path
import re
import sys

from generator_output import write_or_check

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "core/src/main/java/com/gregtech/gregtech/data/generated/MaterialProcessingTargets.java"

KINDS = ["Solidifying", "Bending", "Compressing", "Cutting", "Forging", "Smashing", "Working"]

# GT6's material unit (`gregapi/data/CS.java:120`); the port uses the same value
# (`api/material/GTValues.java:5`), so the extracted amounts are absolute units on both sides.
U = 648_648_000

SETTER = re.compile(r"\.(set%s)\(([^()]*)\)" % "|set".join(KINDS))
# `Field = helper(...)`, `Field, Alias = Field = helper(...)`: the declared field is the owner.
ASSIGN = re.compile(r"^\s*(?P<field>[A-Za-z_]\w*)\s*(?:,\s*[A-Za-z_]\w*\s*)?=")
# `Field.setSolidifying(...)` statements (MT.java:1879-1881 retargets H2O outside its declaration).
STATEMENT = re.compile(r"^\s*(?P<field>[A-Za-z_]\w*)\s*\.(?:set%s)\(" % "|set".join(KINDS))
FIELD = re.compile(r"^[A-Za-z_]\w*$")


def eval_amount(expression: str) -> int:
    """Evaluate one of GT6's amount expressions (`U`, `U2`, `U*9`, `U4*3`, `0`, ...) in units."""
    text = expression.strip()
    if not re.fullmatch(r"[U0-9\s*+()/]+", text):
        sys.exit("cannot evaluate GT6 amount expression %r - the generator needs updating"
                 % expression)
    # U<digits> is GT6's U divided by that number (CS.java:120), U alone is one unit.
    text = re.sub(r"\bU(\d+)\b", lambda m: "(%d//%d)" % (U, int(m.group(1))), text)
    text = re.sub(r"\bU\b", str(U), text)
    try:
        return int(eval(text, {"__builtins__": {}}, {}))
    except Exception:
        sys.exit("cannot evaluate GT6 amount expression %r - the generator needs updating"
                 % expression)


def parse(source: Path):
    """GT6's seven targets: (owner field, kind, target field or '', amount) in source order."""
    rows = []
    for number, line in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
        calls = list(SETTER.finditer(line))
        if not calls:
            continue
        statement = STATEMENT.match(line)
        assign = ASSIGN.match(line)
        if statement:
            owner = statement.group("field")
        elif assign:
            owner = assign.group("field")
        else:
            sys.exit("MT.java:%d sets a processing target without a material field - the "
                     "generator needs updating:\n%s" % (number, line.strip()[:160]))
        for call in calls:
            kind = call.group(1)[len("set"):]
            args = [part.strip() for part in call.group(2).split(",")]
            if len(args) != 2:
                sys.exit("MT.java:%d %s has %d arguments - the generator needs updating"
                         % (number, call.group(1), len(args)))
            target = "" if args[0] == "null" else args[0]
            if target and not FIELD.match(target):
                sys.exit("MT.java:%d %s target %r is not a material field - the generator needs "
                         "updating" % (number, call.group(1), args[0]))
            rows.append((owner, kind, target, eval_amount(args[1])))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="Preserved original gregapi/data/MT.java")
    parser.add_argument("--output", type=Path, default=TARGET)
    parser.add_argument("--check", action="store_true", help="Compare output without writing")
    args = parser.parse_args()
    if args.output.resolve() == args.source.resolve():
        parser.error("Output must not overwrite the original source")
    rows = parse(args.source)
    # GT6's fluent chains override earlier calls, so the last row of an (owner, kind) pair wins.
    latest = {}
    for owner, kind, target, amount in rows:
        latest[(owner, kind)] = (target, amount)
    # Dictionary updates retain the first occurrence's position and the last value.
    ordered = [(owner, kind, target, amount)
               for (owner, kind), (target, amount) in latest.items()]

    owners = sorted({row[0] for row in ordered})
    kinds_used = sorted({row[1] for row in ordered})
    print("GT6 processing target calls: %d (%d rows after override, %d owners, kinds: %s)"
          % (len(rows), len(ordered), len(owners), ", ".join(kinds_used)))
    print("owners: %s" % ", ".join(owners))

    lines = [
        "package com.gregtech.gregtech.data.generated;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/**",
        " * GT6's seven processing targets the port does not model on {@code GTMaterial}:",
        " * " + ", ".join(KINDS) + ".",
        " *",
        " * <p>GT6 keeps all eleven targets on {@code OreDictMaterial} and prints them on the three",
        " * {@code Processing Data} pages of its material dictionary ({@code UT.Books}, the",
        " * {@code addMaterialDictionary} pages); its defaults ({@code OreDictMaterial.java:287-294}) are the",
        " * material itself with one unit, which is what the dictionary falls back to when a material has no",
        " * row here. A target field of {@code null} in GT6 means the material itself as well",
        " * ({@code setBending} and friends substitute {@code this}), and an amount of 0 disables the",
        " * process - both are kept as the original has them.</p>",
        " *",
        " * <p>GENERATED by tools/extract_gt6_processing_targets.py from {@code gregapi/data/MT.java} -",
        " * do not edit by hand.</p>",
        " */",
        "public final class MaterialProcessingTargets {",
        "    private MaterialProcessingTargets() {}",
        "",
        "    /** The seven kinds, in GT6's {@code MT.java} spelling. */",
        "    public static final String[] KINDS = {" + ", ".join('"%s"' % kind for kind in KINDS) + "};",
        "",
        "    /** GT6's default amount for those seven: one material unit. */",
        "    public static final long DEFAULT_AMOUNT = 648_648_000L;",
        "",
        "    /** Rows: {@code ownerField|kind|targetField|amount}; an empty target field is the material itself. */",
        "    public static final String[] ROWS = {",
    ]
    for owner, kind, target, amount in ordered:
        lines.append('            "%s|%s|%s|%d",' % (owner, kind, target, amount))
    lines += [
        "    };",
        "",
        "    /** Material name (sanitized) + kind -> {target field, amount}. */",
        "    private static final Map<String, String[]> BY_MATERIAL = new LinkedHashMap<>();",
        "",
        "    static {",
        "        for (String row : ROWS) {",
        "            String[] parts = row.split(\"\\\\|\", -1);",
        "            String owner = com.gregtech.gregtech.content.recipe.generated.GTMaterialFields.materialOf(parts[0]);",
        "            if (owner == null) continue;   // a GT6 material the port does not register",
        "            BY_MATERIAL.put(com.gregtech.gregtech.api.material.GTMaterial.sanitize(owner)",
        "                    + \"|\" + parts[1], new String[]{parts[2], parts[3]});",
        "        }",
        "    }",
        "",
        "    /**",
        "     * GT6's target of one process for a material: {@code {target field, amount}}, with an empty",
        "     * target field meaning the material itself, or null when GT6 only has the default there.",
        "     */",
        "    public static String[] of(String materialName, String kind) {",
        "        return BY_MATERIAL.get(com.gregtech.gregtech.api.material.GTMaterial.sanitize(materialName)",
        "                + \"|\" + kind);",
        "    }",
        "",
        "    /** How many materials carry an explicit target of the seven. */",
        "    public static int size() { return BY_MATERIAL.size(); }",
        "",
        "    /** How many rows the generated table holds. */",
        "    public static int rowCount() { return ROWS.length; }",
        "}",
        "",
    ]
    write_or_check(args.output, "\n".join(lines), check=args.check)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
