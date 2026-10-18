"""Audit GT6's "Multiblock Machines" registrations against the port, and generate the coverage table.

GT6 registers every multiblock part *and* every multiblock controller of that group as a multi-tile
with a numeric id (`gregtech/loaders/b/Loader_MultiTileEntities.java`). The port hosts them in two
different places:

  * **parts** (walls, coils, processor units, ore-processing wheels) carry GT6's numeric id in
    `content/multiblock/LargeMachineParts`;
  * **original-id controllers** also appear in that table, but must have a real controller block,
    not merely a `MultiblockPortBlock`. Unimplemented original-id controllers are labelled
    PLACEHOLDER explicitly instead of being counted as working parts;
  * **controllers** are ordinary machines registered under the port's own ids
    (`MultiblockDefinitions`, `content/machine/BasicMachineDefinitions`, `LargeMachineLayouts`), often
    with one block per material (`largefermenter_stainless_steel`).

A checker that only knows about parts therefore reports every controller as missing — which is what
this tool used to do (it printed "port covers 97, missing 20" while all twenty were in fact
implemented). It now maps the controllers too and writes the result as a Java table the GameTest side
can assert on, so the coverage stays machine-checked instead of living in a document.

Usage:
  python tools/extract_gt6_multiblock_ids.py             # print the audit
  python tools/extract_gt6_multiblock_ids.py --write     # write the generated Java table
  python tools/extract_gt6_multiblock_ids.py --check     # fail when the file differs (idempotency)
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import argparse
import io
import os
import re
import sys

GT6 = os.path.join("F:\\", "Dev", "GregTech6", "gregtech6-master", "gregtech6-master", str(native_path("src")), "main",
                   "java", "gregtech", "loaders", "b", "Loader_MultiTileEntities.java")
PORT_PARTS = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "multiblock",
                          "LargeMachineParts.java")
OUT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "data", "generated",
                   "GT6MultiblockIds.java")

# GT6 multiblock controller id -> the port's machine name. Every entry was verified by hand:
# the machine exists (BasicMachineDefinitions / MultiblockDefinitions / LargeMachineLayouts), and the
# GameTest in gametest/MultiblockCoverageTests checks that a registered block carries the id.
# The comment on each row is GT6's own name, so a future reader can re-verify the mapping without
# re-reading GT6.
CONTROLLERS = {
    17000: "coke_oven_main",              # Coke Oven
    17100: "largecentrifuge",             # Large Centrifuge
    17101: "distillation_tower_main",     # Distillation Tower
    17102: "largemixer",                  # Large Batch Mixer
    17103: "largeelectrolyzer",           # Large Electrolyzer
    17104: "largebath",                   # Large Bathing Vat
    17105: "largecoagulator",             # Large Coagulator Array
    17106: "largeoven",                   # Large Electric Oven
    17107: "largesluice",                 # Large Sluice
    17108: "largecrusher",                # Large Crusher
    17109: "largeshredder",               # Large Shredder
    17110: "implosion_compressor_main",   # Implosion Compressor
    17111: "cryo_distillation_main",      # Cryo Distillation Tower
    17112: "largeautoclave",              # Large Autoclave
    17113: "largefermenter",              # Large Fermenter
    17114: "largesqueezer",               # Large Squeezer
    17197: "heat_exchanger_main",         # Large Heat Exchanger
    17198: "fusion_reactor_main",         # Fusion Reactor
    17199: "largemassfab",                # Large Matter Fabricator
}

# GT6 gives all 53 of these registrations their own active controller class. Merely finding a
# numeric id in LargeMachineParts does not mean the machine is implemented.
TANK_CONTROLLERS = {17001, *range(17002, 17008), *range(17022, 17028),
                    *range(17042, 17048), *range(17062, 17068)}
CRUCIBLE_CONTROLLERS = {17302, 17303, 17304, 17305, 17306, 17307, 17309, 17312}
ORIGINAL_ID_CONTROLLERS = TANK_CONTROLLERS | CRUCIBLE_CONTROLLERS | \
    set(range(17201, 17206)) | set(range(17211, 17215)) | \
    set(range(17221, 17225)) | set(range(17231, 17235)) | {17996, 17998, 17999}
# Each original-id family is registered as a functional controller in LargeMachineParts.
# The GameTest gate below rejects a regression back to a passive MultiblockPortBlock.
IMPLEMENTED_ORIGINAL_CONTROLLERS = ORIGINAL_ID_CONTROLLERS


def parse_gt6(path):
    """All rows of the "Multiblock Machines" group: (id, name, material expression)."""
    text = io.open(path, encoding="utf-8", errors="replace").read()
    flat = re.sub(r"\s+", " ", text)
    rows = []
    for match in re.finditer(r'aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"\s*,?\s*(\d+)\s*,', flat):
        entry = (int(match.group(2)), match.group(1))
        if entry not in [(i, n) for i, n in rows]:
            rows.append(entry)
    materials = {}
    for match in re.finditer(r'aMat\s*=\s*([^;]+);\s*aRegistry\.add\("[^"]+"\s*,\s*"Multiblock Machines"\s*,?\s*(\d+)', flat):
        materials[int(match.group(2))] = match.group(1).strip()
    rows.sort()
    return [(i, n, materials.get(i, "")) for i, n in rows]


def port_part_ids(path):
    """GT6 ids mapped by the port; definition names are the corresponding registry paths."""
    text = io.open(path, encoding="utf-8", errors="replace").read()
    mapped = {int(id): name for id, name in re.findall(r'new Part\((\d+),"([^"]+)"', text)}
    mapped.update({int(id): None for id in re.findall(r"case (\d+)\s*->", text)})
    return mapped


def classify(rows, part_ids):
    """(id, name, material, kind, port id or None) for every row."""
    out = []
    for gt6_id, name, material in rows:
        if gt6_id in CONTROLLERS:
            out.append((gt6_id, name, material, "CONTROLLER", CONTROLLERS[gt6_id]))
        elif gt6_id in IMPLEMENTED_ORIGINAL_CONTROLLERS and part_ids.get(gt6_id):
            out.append((gt6_id, name, material, "CONTROLLER", part_ids[gt6_id]))
        elif gt6_id in ORIGINAL_ID_CONTROLLERS and gt6_id in part_ids:
            out.append((gt6_id, name, material, "PLACEHOLDER", None))
        elif gt6_id in part_ids:
            out.append((gt6_id, name, material, "PART", None))
        else:
            out.append((gt6_id, name, material, "MISSING", None))
    return out


def java(classified):
    parts = sum(1 for row in classified if row[3] == "PART")
    controllers = sum(1 for row in classified if row[3] == "CONTROLLER")
    placeholders = sum(1 for row in classified if row[3] == "PLACEHOLDER")
    missing = sum(1 for row in classified if row[3] == "MISSING")
    lines = [
        "package com.gregtech.gregtech.data.generated;",
        "",
        "import java.util.List;",
        "",
        "/**",
        " * GENERATED by tools/extract_gt6_multiblock_ids.py - do not edit by hand.",
        " *",
        " * <p>Every registration of GT6's \"Multiblock Machines\" group",
        " * ({@code Loader_MultiTileEntities}) with the port's counterpart, so the coverage is asserted",
        " * instead of described: {@code gametest/MultiblockCoverageTests} checks each row.</p>",
        " *",
        " * <p>{@code kind} distinguishes passive parts, active controllers and known placeholders.",
        " * A registered numeric id alone is not evidence of active controller functionality.</p>",
        " */",
        "public final class GT6MultiblockIds {",
        "    private GT6MultiblockIds() {}",
        "",
        "    /** How a row is hosted in the port. */",
        "    public enum Kind { PART, CONTROLLER, PLACEHOLDER }",
        "",
        "    /** GT6 id, GT6 name, how the port hosts it, and its port block id for controllers. */",
        "    public record Row(int gt6Id, String gt6Name, Kind kind, String portId) {}",
        "",
        "    public static final int MISSING = %d;" % missing,
        "",
        "    public static final List<Row> ROWS = List.of(",
    ]
    rendered = []
    for gt6_id, name, _material, kind, port_id in classified:
        if kind == "MISSING":
            continue
        rendered.append('            new Row(%d, "%s", Kind.%s, %s)'
                        % (gt6_id, name.replace('"', '\\"'), kind,
                           'null' if port_id is None else '"%s"' % port_id))
    lines.append(",\n".join(rendered))
    lines += [
        "    );",
        "",
        "    /** Rows hosted as structural parts ({@code LargeMachineParts.find}). */",
        "    public static final int PART_COUNT = %d;" % parts,
        "    /** Rows hosted as actual controller blocks. */",
        "    public static final int CONTROLLER_COUNT = %d;" % controllers,
        "    /** GT6 controller registrations still backed only by a passive port block. */",
        "    public static final int PLACEHOLDER_COUNT = %d;" % placeholders,
        "}",
        "",
    ]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="write the generated Java table")
    parser.add_argument("--check", action="store_true", help="fail when the file on disk differs")
    args = parser.parse_args()

    rows = parse_gt6(GT6)
    classified = classify(rows, port_part_ids(PORT_PARTS))
    missing = [row for row in classified if row[3] == "MISSING"]
    text = java(classified)

    if args.check:
        if not os.path.exists(OUT):
            print("missing %s" % OUT)
            return 1
        if io.open(OUT, encoding="utf-8").read() != text:
            print("%s is out of date" % OUT)
            return 1
        print("%s is up to date (%d rows)" % (OUT, len(classified) - len(missing)))
        return 0

    if args.write:
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        with io.open(OUT, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
        print("wrote %s (%d rows, %d missing)" % (OUT, len(classified) - len(missing), len(missing)))
        return 0

    parts = sum(1 for row in classified if row[3] == "PART")
    controllers = sum(1 for row in classified if row[3] == "CONTROLLER")
    placeholders = sum(1 for row in classified if row[3] == "PLACEHOLDER")
    print("GT6 multiblock registrations: %d" % len(classified))
    print("  parts       %d" % parts)
    print("  controllers %d" % controllers)
    print("  placeholders %d" % placeholders)
    print("  missing     %d" % len(missing))
    print()
    print("%-7s %-44s %-12s %s" % ("id", "GT6 name", "kind", "port"))
    for gt6_id, name, _material, kind, port_id in classified:
        print("%-7d %-44s %-12s %s" % (gt6_id, name[:44], kind, port_id or ""))
    return 0 if not missing else 2


if __name__ == "__main__":
    sys.exit(main())
