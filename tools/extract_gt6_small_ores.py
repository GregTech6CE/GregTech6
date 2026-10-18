"""Extract GT6's WorldgenOresSmall registrations (Loader_Worldgen) with their dimension flags.

Prints one line per registration plus a per-dimension summary, so the port's
GTOreVeins.OVERWORLD_SMALL_ORES / NETHER_SMALL_ORES / END_SMALL_ORES tables can be diffed
against the original.

Usage: python tools/extract_gt6_small_ores.py [--json out.json]
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


def parse_registrations():
    text = io.open(GT6, encoding="utf-8", errors="replace").read()
    # The lines can wrap; join them by the 'new WorldgenOresSmall(' marker.
    flat = re.sub(r"\s+", " ", text)
    out = []
    for match in re.finditer(r"new WorldgenOresSmall\((.*?)\)\s*;", flat):
        body = match.group(1)
        args = []
        depth = 0
        current = ""
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
        if len(args) < 6:
            continue
        name = args[0].strip('"')
        if name.startswith("ore.small."):
            name = name[len("ore.small."):]
        out.append({
            "name": name,
            "default": args[1],
            "minY": args[2],
            "maxY": args[3],
            "amount": args[4],
            "material": args[5],
            "dims": [d for d in DIMS if any(a.strip() == d for a in args[6:])],
            "otherDims": [a.strip() for a in args[6:] if a.strip() not in DIMS and a.strip().startswith("GEN_")],
        })
    return out


def parse_port():
    """Reads the port's three tables: name -> (minY, maxY, amount)."""
    text = io.open(PORT, encoding="utf-8", errors="replace").read()
    tables = {}
    for table in ["OVERWORLD_SMALL_ORES", "NETHER_SMALL_ORES", "END_SMALL_ORES"]:
        start = text.find(table + " = List.of(")
        if start < 0:
            tables[table] = {}
            continue
        end = text.find(");", start)
        body = text[start:end]
        entries = {}
        for m in re.finditer(r'new SmallOre\("([a-z0-9_]+)",\s*(\d+),\s*(\d+),\s*(\d+)', body):
            entries[m.group(1)] = (int(m.group(2)), int(m.group(3)), int(m.group(4)))
        tables[table] = entries
    return tables


def short_material(expr):
    expr = expr.strip()
    expr = expr.replace("com.gregtech.gregtech.content.material.generated.OreMaterials.", "OM.")
    expr = expr.replace("gregapi.data.", "")
    return expr


def main():
    regs = parse_registrations()
    tables = parse_port()
    dim_of = {"GEN_OVERWORLD": "OVERWORLD_SMALL_ORES", "GEN_NETHER": "NETHER_SMALL_ORES",
              "GEN_END": "END_SMALL_ORES"}

    print("GT6 WorldgenOresSmall registrations: %d" % len(regs))
    report = {}
    for dim in DIMS:
        gt6 = {}
        gated = {}
        for reg in regs:
            if dim not in reg["dims"]:
                continue
            flag = reg["default"].strip()
            if flag in ("F", "false"):
                continue  # dead registration in GT6 itself
            if flag not in ("T", "true"):
                gated[reg["name"]] = flag  # gated on another mod being loaded
                continue
            gt6[reg["name"]] = reg
        port = tables[dim_of[dim]]
        missing = sorted(set(gt6) - set(port))
        extra = sorted(set(port) - set(gt6))
        mismatch = []
        for name in sorted(set(gt6) & set(port)):
            reg = gt6[name]
            want = tuple(int(reg[k]) for k in ("minY", "maxY", "amount"))
            if want != port[name]:
                mismatch.append("%s GT6=%s port=%s" % (name, want, port[name]))
        report[dim] = {"gt6": len(gt6), "gt6Gated": len(gated), "port": len(port),
                       "missing": missing, "extra": extra, "mismatch": mismatch,
                       "gated": gated}
        print("\n== %s: GT6 %d always-on (+%d other-mod gated) / port %d"
              % (dim, len(gt6), len(gated), len(port)))
        print("  missing in port (%d): %s" % (len(missing), ", ".join(missing) or "-"))
        print("  port-only (%d): %s" % (len(extra), ", ".join(extra) or "-"))
        print("  Y/amount mismatches (%d): %s" % (len(mismatch), "; ".join(mismatch) or "-"))
        print("  other-mod gated: %s" % ", ".join(sorted(gated)) or "-")

    if "--json" in sys.argv:
        path = sys.argv[sys.argv.index("--json") + 1]
        payload = {"registrations": regs, "report": report}
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(payload, handle, indent=2)
            handle.write("\n")
        print("\nwrote %s" % path)


if __name__ == "__main__":
    main()
