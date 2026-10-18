"""Estimate what aligning the ore / machine-casing block conditions with GT6 would add.

GT6 conditions (from ``docs/prefix-condition-comparison.json``): ``ore``/``oreSmall`` = ``ORES``,
``casingMachine*`` = ``PARTS``. The port decides both from its own ``MaterialProperty`` values, so this
counts, per GT6-id joined material:

  * which port factory declares it (the port's factories imply the properties),
  * whether GT6 gives it the flag,
  * and therefore how many materials would newly get an ore block / a machine casing.

Usage:  python tools/report_block_condition_delta.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import pathlib
import re

GT6_MT = pathlib.Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
FACTORIES = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/api/material/MaterialFactories.java"))

GT6_DECL = re.compile(r"^\s*(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*(\w+)\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
PORT_PUT = re.compile(r"(\w+)\.put\(([^)]*)\)")


def main() -> None:
    gt6 = {int(i): (f, d) for f, i, d in GT6_DECL.findall(
        GT6_MT.read_text(encoding="utf-8", errors="replace"))}
    port: dict[int, tuple[str, str, str]] = {}
    for path in sorted(PORT_MATERIALS.rglob("*.java")):
        for name, factory, port_id, display in PORT_DECL.findall(
                path.read_text(encoding="utf-8", errors="replace")):
            port.setdefault(int(port_id), (name, factory, display))

    flags_by_id: dict[int, set[str]] = {}
    for line in re.findall(r'^\s+"(\d+)\|([A-Z_,]*)"', FORMS.read_text(encoding="utf-8"), re.M):
        flags_by_id[int(line[0])] = set(line[1].split(",")) if line[1] else set()

    factory_body = FACTORIES.read_text(encoding="utf-8")
    ore_factories = set(re.findall(r"public static GTMaterial (\w+)\(", factory_body))

    counts = {"ores": 0, "oreDelta": 0, "parts": 0, "partsDelta": 0}
    ore_examples, parts_examples = [], []
    for port_id, (name, factory, display) in sorted(port.items()):
        flags = flags_by_id.get(port_id, set())
        if "ORES" in flags:
            counts["ores"] += 1
            if factory not in {"ore", "gemOre", "oreNether"}:
                counts["oreDelta"] += 1
                ore_examples.append((port_id, name, factory))
        if "PARTS" in flags:
            counts["parts"] += 1
            if factory not in {"metal", "alloy", "metalmachine", "alloymachine"}:
                counts["partsDelta"] += 1
                parts_examples.append((port_id, name, factory))

    print(f"port materials joined by GT6 id: {len(port)}")
    print(f"  with the ORES flag: {counts['ores']}; declared through a non-ore factory: {counts['oreDelta']}")
    print("    " + ", ".join(f"{name}({factory})" for _id, name, factory in ore_examples[:12]))
    print(f"  with the PARTS flag: {counts['parts']}; declared through a non-metal factory: "
          f"{counts['partsDelta']}")
    print("    " + ", ".join(f"{name}({factory})" for _id, name, factory in parts_examples[:12]))
    print(f"port factories available: {len(ore_factories)}")


if __name__ == "__main__":
    main()
