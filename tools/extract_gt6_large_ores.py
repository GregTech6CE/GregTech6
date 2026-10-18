"""Audit GT6's WorldgenOresLarge registrations against the port's vein tables.

GT6: `new WorldgenOresLarge(name, aDefault, aIndicator, minY, maxY, weight, count, size,
material, material1, material2, material3, dims...)` (Loader_Worldgen:885-927). The port keeps
per-dimension tables in GTOreVeins; this prints the per-dimension diff.

Usage: python tools/extract_gt6_large_ores.py [--json out.json]
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os
import re
import sys

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregtech\loaders\b\Loader_Worldgen.java"
PORT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen", "GTOreVeins.java")
DIMS = ["GEN_OVERWORLD", "GEN_NETHER", "GEN_END"]
# WorldgenOresLarge gets its dimension membership from the ORE_* lists at the end of the call.
ORE_SETS = {"OVERWORLD": "ORE_OVERWORLD", "NETHER": "ORE_NETHER", "END": "ORE_END"}


def split_args(body):
    args, depth, current = [], 0, ""
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            args.append(current.strip())
            current = ""
        else:
            current += ch
    args.append(current.strip())
    return args


def parse_registrations():
    text = io.open(GT6, encoding="utf-8", errors="replace").read()
    flat = re.sub(r"\s+", " ", text)
    out = []
    for match in re.finditer(r"new WorldgenOresLarge\((.*?)\)\s*;", flat):
        args = split_args(match.group(1))
        if len(args) < 9:
            continue
        name = args[0].strip('"')
        if name.startswith("ore.large."):
            name = name[len("ore.large."):]
        out.append({
            "name": name,
            "default": args[1],
            "indicator": args[2],
            "minY": args[3],
            "maxY": args[4],
            "weight": args[5],
            "count": args[6],
            "size": args[7],
            "materials": [a.strip() for a in args[8:12]],
            "dims": [d for d in DIMS if any(a.strip() == ORE_SETS[d.replace("GEN_", "")]
                                            for a in args[12:])],
            "oreSets": sorted({a.strip() for a in args[12:] if a.strip().startswith("ORE_")}),
        })
    return out


def parse_port():
    text = io.open(PORT, encoding="utf-8", errors="replace").read()
    tables = {}
    for table in ["OVERWORLD_VEINS", "NETHER_VEINS", "END_VEINS"]:
        start = text.find(table + " = List.of(")
        if start < 0:
            tables[table] = {}
            continue
        end = text.find("    );", start)
        body = text[start:end]
        entries = {}
        for m in re.finditer(r'new OreVein\("([a-z0-9_]+)",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)', body):
            entries[m.group(1)] = tuple(int(m.group(i)) for i in range(2, 7))
        tables[table] = entries
    return tables


def main():
    regs = parse_registrations()
    tables = parse_port()
    dim_of = {"GEN_OVERWORLD": "OVERWORLD_VEINS", "GEN_NETHER": "NETHER_VEINS", "GEN_END": "END_VEINS"}
    print("GT6 WorldgenOresLarge registrations: %d" % len(regs))
    report = {}
    for dim in DIMS:
        gt6, gated = {}, {}
        for reg in regs:
            if dim not in reg["dims"]:
                continue
            flag = reg["default"].strip()
            if flag in ("F", "false"):
                continue
            if flag not in ("T", "true"):
                gated[reg["name"]] = flag
                continue
            gt6[reg["name"]] = reg
        port = tables[dim_of[dim]]
        missing = sorted(set(gt6) - set(port))
        extra = sorted(set(port) - set(gt6))
        mismatch = []
        for name in sorted(set(gt6) & set(port)):
            reg = gt6[name]
            want = (int(reg["minY"]), int(reg["maxY"]), int(reg["weight"]), int(reg["count"]), int(reg["size"]))
            if want != port[name]:
                mismatch.append("%s GT6=%s port=%s" % (name, want, port[name]))
        report[dim] = {"gt6": len(gt6), "gated": sorted(gated), "port": len(port),
                       "missing": missing, "extra": extra, "mismatch": mismatch}
        print("\n== %s: GT6 %d always-on (+%d gated) / port %d"
              % (dim, len(gt6), len(gated), len(port)))
        print("  missing in port (%d): %s" % (len(missing), ", ".join(missing) or "-"))
        print("  port-only (%d): %s" % (len(extra), ", ".join(extra) or "-"))
        print("  parameter mismatches (%d): %s" % (len(mismatch), "; ".join(mismatch) or "-"))
        print("  other-mod gated: %s" % (", ".join(sorted(gated)) or "-"))

    if "--json" in sys.argv:
        path = sys.argv[sys.argv.index("--json") + 1]
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            json.dump({"registrations": regs, "report": report}, handle, indent=2)
            handle.write("\n")
        print("\nwrote %s" % path)


if __name__ == "__main__":
    main()
