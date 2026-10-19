"""Extract GT6 material processing flags from MT.java.

GT6 uses these flags for a lot of machine handler decisions:

  * ``SOFT`` / ``FURNACE`` — ``tEasyWorkable`` (Loader_Recipes_Handlers:58): easy materials get fixed
    compression durations (16 / 144 ticks), hard ones pay 256*(1+toolQuality) per material unit.
  * ``MORTAR`` — "can be ground in a mortar" (TD.java:515); most Mortar handler rows
    (Loader_Recipes_Handlers:80-112) require it; the rockGt row at :79 does not.
  * ``BRITTLE`` / ``FOOD`` — the extra Mortar conditions for gems, sticks, ingots and plates
    (``Or(BRITTLE, FOOD, <finer form>.NOT)`` / ``Or(BRITTLE, FOOD, WOOD)``).

The port has no FURNACE/SOFT/MORTAR/BRITTLE/FOOD material property, so the flags are imported as
generated name tables instead of inventing a new material property.

Usage: python tools/extract_gt6_workability.py --source <MT.java> --check

The legacy algorithm does not propagate factory flags and uses field-name keys
(ALIGN-010). Check mode proves the existing table can be reproduced, not that its
material membership is complete. This source generator writes directly to core.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re

from generator_output import write_or_check

ROOT = Path(__file__).resolve().parents[1]
OUT_JAVA = ROOT / "core/src/main/java/com/gregtech/gregtech/data/generated/MaterialWorkability.java"

DECL_ENUM = re.compile(r"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=")
DECL_STATIC = re.compile(r"OreDictMaterial\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(")
FLAGS = ("SOFT", "FURNACE", "MORTAR", "BRITTLE", "FOOD", "CRYSTALLISABLE")


def normalize(name: str) -> str:
    return re.sub(r"[^a-z0-9]", "", name.lower())


def statements(text: str) -> list[str]:
    """Join physical lines into declaration statements by paren balance."""
    out: list[str] = []
    buf = ""
    depth = 0
    for raw in text.splitlines():
        line = raw.split("//")[0]
        if not buf and not line.strip():
            continue
        buf += line + "\n"
        depth += line.count("(") - line.count(")")
        if depth <= 0:
            if buf.strip():
                out.append(buf)
            buf = ""
            depth = 0
    if buf.strip():
        out.append(buf)
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="Preserved original gregapi/data/MT.java")
    parser.add_argument("--output", type=Path, default=OUT_JAVA)
    parser.add_argument("--report", type=Path, help="Optional extraction report; historical reports are not overwritten by default")
    parser.add_argument("--check", action="store_true", help="Compare requested outputs without writing")
    args = parser.parse_args()
    if any(path is not None and path.resolve() == args.source.resolve() for path in (args.output, args.report)):
        parser.error("Outputs must not overwrite the original source")
    if args.report is not None and args.report.resolve() == args.output.resolve():
        parser.error("Java output and extraction report must use different files")
    text = args.source.read_text(encoding="utf-8")

    soft: dict[str, str] = {}
    furnace: dict[str, str] = {}
    flags: dict[str, dict[str, str]] = {name: {} for name in FLAGS}
    multi = 0
    for stmt in statements(text):
        decls: list[str] = []
        m = DECL_STATIC.search(stmt)
        if m:
            decls.append(m.group(1))
        else:
            for line in stmt.splitlines():
                m2 = DECL_ENUM.match(line)
                if m2:
                    decls.append(m2.group(1))
        if not decls:
            continue
        if len(decls) > 1:
            multi += 1
        present = {name: bool(re.search(r"(?<![A-Z_])" + name + r"(?![A-Z_])", stmt)) for name in FLAGS}
        if not any(present.values()):
            continue
        for name in decls:
            key = normalize(name)
            if present["SOFT"]:
                soft[key] = name
            if present["FURNACE"]:
                furnace[key] = name
            for flag, hit in present.items():
                if hit:
                    flags[flag][key] = name

    easy = sorted(set(soft) | set(furnace))
    lines = [
        "package com.gregtech.gregtech.data.generated;",
        "",
        "import com.gregtech.gregtech.api.material.GTMaterial;",
        "import com.gregtech.gregtech.api.material.MaterialProperty;",
        "",
        "import java.util.Set;",
        "",
        "/**",
        " * GT6 material processing flags ({@code SOFT}, {@code FURNACE}, {@code MORTAR}, {@code BRITTLE},",
        " * {@code FOOD}) from {@code MT.java}.",
        " * <p>",
        " * GT6 {@code Loader_Recipes_Handlers:58} defines",
        " * {@code Or tEasyWorkable = new Or<>(FURNACE, SOFT)} and uses it to pick between a fixed",
        " * and a quality-scaled duration in the compression handlers (same file, lines 217-233);",
        " * {@code MORTAR} (TD.java:515, \"can be ground in a mortar\") gates the whole Mortar handler",
        " * block (same file, lines 79-112), whose gem/stick/ingot/plate rows additionally ask for",
        " * {@code BRITTLE} or {@code FOOD}.",
        " * Names are normalised (lowercased, non-alphanumerics dropped) because GT6's internal",
        " * symbol and the port's registry name are not spelled identically.",
        " * <p>",
        " * GENERATED by tools/extract_gt6_workability.py - do not edit by hand.",
        " */",
        "public final class MaterialWorkability {",
        "    private MaterialWorkability() {}",
        "",
        f"    private static final Set<String> SOFT = Set.of({_literals(sorted(soft))});",
        f"    private static final Set<String> FURNACE = Set.of({_literals(sorted(furnace))});",
        f"    private static final Set<String> MORTAR = Set.of({_literals(sorted(flags['MORTAR']))});",
        f"    private static final Set<String> BRITTLE = Set.of({_literals(sorted(flags['BRITTLE']))});",
        f"    private static final Set<String> FOOD = Set.of({_literals(sorted(flags['FOOD']))});",
        f"    private static final Set<String> CRYSTALLISABLE = Set.of({_literals(sorted(flags['CRYSTALLISABLE']))});",
        "",
        "    public static boolean isSoft(GTMaterial material) {",
        "        return material != null && SOFT.contains(normalize(material.getName()));",
        "    }",
        "",
        "    public static boolean isFurnace(GTMaterial material) {",
        "        return material != null && FURNACE.contains(normalize(material.getName()));",
        "    }",
        "",
        "    /** GT6 {@code TD.Processing.MORTAR}: the material can be ground by a mortar. */",
        "    public static boolean isMortarGrindable(GTMaterial material) {",
        "        // MT.wood/woodnormal grant MORTAR in their factory, outside declaration flags.",
        "        return material != null && (material.has(MaterialProperty.WOOD)",
        "                || MORTAR.contains(normalize(material.getName())));",
        "    }",
        "",
        "    /** GT6 {@code TD.Properties.BRITTLE}. */",
        "    public static boolean isBrittle(GTMaterial material) {",
        "        return material != null && BRITTLE.contains(normalize(material.getName()));",
        "    }",
        "",
        "    /** GT6 {@code TD.Properties.FOOD}. */",
        "    public static boolean isFood(GTMaterial material) {",
        "        return material != null && FOOD.contains(normalize(material.getName()));",
        "    }",
        "",
        "    /** GT6 {@code TD.Processing.CRYSTALLISABLE}: the material grows crystals in the autoclave. */",
        "    public static boolean isCrystallisable(GTMaterial material) {",
        "        return material != null && CRYSTALLISABLE.contains(normalize(material.getName()));",
        "    }",
        "",
        "    /** GT6 {@code tEasyWorkable}: fixed machine durations instead of quality-scaled ones. */",
        "    public static boolean isEasyWorkable(GTMaterial material) {",
        "        return isSoft(material) || isFurnace(material);",
        "    }",
        "",
        "    public static String normalize(String name) {",
        "        StringBuilder builder = new StringBuilder(name.length());",
        "        for (int i = 0; i < name.length(); i++) {",
        "            char c = Character.toLowerCase(name.charAt(i));",
        "            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') builder.append(c);",
        "        }",
        "        return builder.toString();",
        "    }",
        "}",
    ]
    write_or_check(args.output, "\n".join(lines) + "\n", check=args.check)

    report = {
        "source": str(args.source.resolve()),
        "soft": sorted(soft.values()),
        "furnace": sorted(furnace.values()),
        "easyWorkable": [soft.get(k) or furnace[k] for k in easy],
        "mortarGrindable": sorted(flags["MORTAR"].values()),
        "crystallisable": sorted(flags["CRYSTALLISABLE"].values()),
        "brittle": sorted(flags["BRITTLE"].values()),
        "food": sorted(flags["FOOD"].values()),
        "statementsWithMultipleDeclarations": multi,
    }
    if args.report is not None:
        write_or_check(args.report, json.dumps(report, indent=2, ensure_ascii=False) + "\n", check=args.check)

    print(f"SOFT: {len(soft)}  FURNACE: {len(furnace)}  easy workable union: {len(easy)}")
    print(f"MORTAR: {len(flags['MORTAR'])}  BRITTLE: {len(flags['BRITTLE'])}  FOOD: {len(flags['FOOD'])}"
          f"  CRYSTALLISABLE: {len(flags['CRYSTALLISABLE'])}")
    print("multi-declaration statements seen: " + str(multi))


def _literals(names: list[str]) -> str:
    if not names:
        return ""
    chunks = ['"' + n + '"' for n in names]
    out = ""
    line = ""
    for chunk in chunks:
        if len(line) + len(chunk) + 2 > 116:
            out += line.rstrip() + "\n"
            line = "                "
        line += chunk + ", "
    return (out + line.rstrip().rstrip(",")) if out else line.rstrip().rstrip(",")


if __name__ == "__main__":
    main()
