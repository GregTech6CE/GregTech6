"""Census of GT6 machine registrations versus the port's machine definitions.

GT6 registers every machine through ``aRegistry.add("<Name>", "<Category>", <id>, ...)``
in ``gregtech/loaders/b/Loader_MultiTileEntities.java``. The port declares its
single-block machines in ``content/machine/BasicMachineDefinitions.java`` and its
multiblocks per family under ``content/multiblock/`` (plus hand-written
``BasicMachineOriginalParams``/``MultiblockRecipePack`` tables).

Usage: python tools/census_gt6_registrations.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import collections
import json
import re
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parents[1]
GT6 = Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
GT6_REGISTRY = GT6 / "gregtech/loaders/b/Loader_MultiTileEntities.java"
GT6_RECIPE_MAPS = GT6 / "gregapi/data/RM.java"
PORT = native_path(ROOT / "src/main/java/com/gregtech/gregtech")
PORT_MACHINES = PORT / "content/machine/BasicMachineDefinitions.java"
PORT_MAPS = PORT / "data/MachineRecipeMaps.java"
PORT_MULTIBLOCK = PORT / "content/multiblock"


def gt6_categories() -> tuple[collections.Counter, dict[str, set[str]]]:
    """GT6 builds most names by concatenation, e.g. ``"Lathe (" + aMat.getLocal() + ")"``.

    So the leading string literal of the first argument is the machine name (possibly followed by
    the tier/material suffix), and the second literal is the registry category.
    """
    src = GT6_REGISTRY.read_text(encoding="utf-8", errors="replace")
    entries = re.findall(r'aRegistry\.add\(\s*"([^"]*)"[^,]*,\s*"([^"]+)"', src)
    cats = collections.Counter(cat for _, cat in entries)
    names: dict[str, set[str]] = collections.defaultdict(set)
    for name, cat in entries:
        names[cat].add(base_name(name))
    return cats, names


def base_name(raw: str) -> str:
    """``"Lathe ("`` -> ``Lathe``; ``"Cryo Mixer (T1)"`` -> ``Cryo Mixer``."""
    name = re.sub(r"\s*\(T\d\)\s*$", "", raw).strip()
    name = re.sub(r"\s*\(\s*$", "", name).strip()
    return name


def gt6_map_count() -> int:
    src = GT6_RECIPE_MAPS.read_text(encoding="utf-8", errors="replace")
    return len(re.findall(r"(?m)^\s*,?\s*[A-Z][A-Za-z0-9_]*\s*=\s*new RecipeMap", src))


def port_map_count() -> int:
    src = PORT_MAPS.read_text(encoding="utf-8", errors="replace")
    return len(re.findall(r"(?m)^\s*,?\s*[A-Z][A-Za-z0-9_]*\s*=\s*new RecipeMap", src))


def port_machines() -> list[str]:
    src = PORT_MACHINES.read_text(encoding="utf-8", errors="replace")
    return sorted(set(re.findall(r'name\("([a-z0-9_]+)"\)', src))
                  | set(re.findall(r'"([a-z0-9_]+)"\s*,\s*"[^"]*"\s*,', src)))


def port_multiblock_families() -> list[str]:
    return sorted(p.stem for p in PORT_MULTIBLOCK.glob("*.java"))


def port_machine_names() -> set[str]:
    src = PORT_MACHINES.read_text(encoding="utf-8", errors="replace")
    return set(re.findall(r'new MachineDef\(\s*"([^"]+)"', src))


def normalize(name: str) -> str:
    """Drop GT6's "(T1)".."(T5)" tier suffix and compare on letters/digits only."""
    return re.sub(r"[^a-z0-9]", "", re.sub(r"\s*\(T\d\)\s*$", "", name).lower())


# GT6 name -> port name, where the port implements the same machine under another name.
# Every entry was verified against the port's MachineDef list; the port uses the GT6 recipe map
# in each case (e.g. the port's "debarker" machine is GT6's "Pressure Washer", same map).
NAME_ALIASES = {
    "burnermixer": "burnmixer",
    "canningmachine": "canner",
    "lightningprocessor": "lightning",
    "matterfabricator": "massfab",
    "matterreplicator": "replicator",
    "molecularscanner": "scannermolecular",
    "nanoscalefabricator": "nanofab",
    "pressurewasher": "debarker",
    "roastingoven": "roaster",
    "sandingmachine": "sander",
    "lowheatextruder": "extruder",
    "scannervisuals": "scannervisuals",
}


def main() -> None:
    cats, names = gt6_categories()
    basic = names.get("Basic Machines", set())
    multi = names.get("Multiblock Machines", set())
    port_names = port_machine_names()
    port_norm = {normalize(n) for n in port_names}
    missing_basic = sorted({base_name(n) for n in basic if normalize(n) not in port_norm
                            and NAME_ALIASES.get(normalize(n), normalize(n)) not in port_norm})
    multi_machines = sorted(n for n in multi if n.startswith("Large") or n.endswith("Controller"))
    multi_parts = sorted(n for n in multi if n not in multi_machines)
    missing_multi = sorted(n for n in multi_machines if normalize(n) not in port_norm)
    report = {
        "gt6RegistrationsByCategory": dict(cats.most_common()),
        "gt6DistinctNames": {cat: len(members) for cat, members in sorted(names.items())},
        "gt6RecipeMaps": gt6_map_count(),
        "portRecipeMaps": port_map_count(),
        "portMachineNames": sorted(port_names),
        "gt6BasicMachines": sorted({re.sub(r"\s*\(T\d\)\s*$", "", n) for n in basic}),
        "gt6MultiblockMachines": multi_machines,
        "gt6MultiblockParts": multi_parts,
        "missingBasicMachines": missing_basic,
        "missingMultiblockMachines": missing_multi,
        "portMultiblockFiles": port_multiblock_families(),
    }
    print("GT6 registrations by category (count / distinct names):")
    for cat, count in cats.most_common():
        print(f"  {count:5} / {len(names[cat]):4}  {cat}")
    print(f"\nrecipe maps: GT6 {report['gt6RecipeMaps']} vs port {report['portRecipeMaps']}")
    print(f"port machine definitions: {len(port_names)}")
    print(f"GT6 basic machine types: {len(report['gt6BasicMachines'])}"
          f" -> missing in port: {len(missing_basic)}")
    print("  " + "; ".join(missing_basic))
    print(f"GT6 multiblock machines (Large*/Controller): {len(multi_machines)}"
          f" -> missing in port: {len(missing_multi)}")
    print("  " + "; ".join(missing_multi))
    print(f"GT6 multiblock structure parts: {len(multi_parts)}")
    print(f"\nport multiblock definition files: {len(report['portMultiblockFiles'])}")
    (ROOT / "docs/gt6-registration-census.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print("\nwrote docs/gt6-registration-census.json")


if __name__ == "__main__":
    main()
