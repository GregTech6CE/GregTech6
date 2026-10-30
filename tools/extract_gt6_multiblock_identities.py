#!/usr/bin/env python3
"""Reproduce GT6's multi-tile form identities (pipes, wires, cells) from the pinned source.

Usage: python tools/extract_gt6_multiblock_identities.py --source <GT6>/src/main/java [--check]

GT6 registers pipes, wires and capsule cells as ``gregtech:gt.multitileentity`` metas, so a recipe
dump only shows a numeric meta. The port registers one block per form pair. This script turns the
original's registration code into ``meta -> "<form>#<material id>"`` so
``tools/compare_gt6_recipes.py`` can compare both sides by the same form identity:

* fluid pipes: ``MultiTileEntityPipeFluid.addFluidPipes(base, ...)`` — base+0..6 are
  tiny/small/medium/large/huge/quadruple/nonuple.
* item pipes: ``MultiTileEntityPipeItem.addItemPipes(base, ...)`` — base+2..7 are
  medium/large/huge/restrictive medium/large/huge. GT6 reuses the same ``OP.pipe*`` prefix for item
  and fluid pipes, so the identity carries the item prefix to keep the two registrations apart.
* electric wires: ``MultiTileEntityWireElectric.addElectricWires(base, ...)`` — base+0..15 are
  wireGt01..16, base+16/17/19/23/27 are cableGt01/02/04/08/12.
* redstone wires: per-material ``MultiTileEntityWireRedstone[Insulated]`` registrations in the
  27000 band (wireGt01 / cableGt01).
* capsule cells: ``MultiTileEntityCell`` registrations (capcellcon).

Material ids are resolved through the MT factory chain, the same way ``extract_gt6_form_flags.py``
does. Unreviewed source versions fail closed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT_JSON = os.path.join(ROOT, "tools", "gt6_multiblock_identities.json")

PINNED = {
    "Loader_MultiTileEntities.java": "96f579dd5c0a26a759ddee34c312dadbc5d52123e2b85e82da6a40f68b494393",
    "MultiTileEntityWireElectric.java": "60dd3bf9147a220ab327a1aed3f0081382678eb0d6190e865e84e1b7dae89724",
    "MultiTileEntityPipeItem.java": "0e9aa956b807d8390b20dd70f224b2b8d0a7be8df25fe66660abffda8b88069b",
    "MultiTileEntityPipeFluid.java": "3a6850055375e7975145b03abab44c5922746a6f4053dd8d3163de989f37b110",
    "MT.java": "12936831b1b5f419dc783c5f32f3cc07a329734ca6c5c11757d52e1be27904b2",
}

FLUID_PIPE_SIZES = ("Tiny", "Small", "Medium", "Large", "Huge", "Quadruple", "Nonuple")
ITEM_PIPE_OFFSETS = {
    2: "ItemPipeMedium",
    3: "ItemPipeLarge",
    4: "ItemPipeHuge",
    5: "ItemPipeRestrictiveMedium",
    6: "ItemPipeRestrictiveLarge",
    7: "ItemPipeRestrictiveHuge",
}
CABLE_OFFSETS = {16: 1, 17: 2, 19: 4, 23: 8, 27: 12}
# GT6 declares MT.Superconductor through tier(), i.e. id -1 with a name only; the port carries it as
# material 9516 and WireCatalog names MT.Superconductor for that entry.
FIELD_ID_OVERRIDES = {"Superconductor": 9516}


def read_source(path: str) -> str:
    with open(path, "rb") as handle:
        raw = handle.read().replace(b"\r\n", b"\n")
    expected = PINNED.get(os.path.basename(path))
    if expected and hashlib.sha256(raw).hexdigest() != expected:
        raise SystemExit("Unreviewed %s version; expected GT6 3703e403" % os.path.basename(path))
    return raw.decode("utf-8", errors="replace")


def material_ids(mt_text: str) -> dict[str, int]:
    """MT field name -> GT6 material id, following helper factories that hide the id."""
    bodies: dict[str, list[str]] = {}
    for match in re.finditer(r"static OreDictMaterial\s+(\w+)\s*\([^)]*\)[^{]*\{", mt_text):
        start = mt_text.index("{", match.end() - 1)
        depth = 0
        for index in range(start, len(mt_text)):
            if mt_text[index] == "{":
                depth += 1
            elif mt_text[index] == "}":
                depth -= 1
                if depth == 0:
                    bodies.setdefault(match.group(1), []).append(mt_text[start : index + 1])
                    break

    def factory_id(name: str, seen: tuple[str, ...] = ()) -> int | None:
        for body in bodies.get(name, []):
            literal = re.search(r"\(\s*(\d+)\s*,\s*\"", body)
            if literal:
                return int(literal.group(1))
            for callee in re.findall(r"(?<![.\w])(\w+)\s*\(", body):
                if callee in bodies and callee not in seen:
                    value = factory_id(callee, seen + (name,))
                    if value:
                        return value
        return None

    ids: dict[str, int] = {}
    for line in mt_text.splitlines():
        for match in re.finditer(r"(?<![.\w])(\w+)\s*=\s*\w+\s*\(\s*(\d+)\s*,\s*\"", line):
            ids.setdefault(match.group(1), int(match.group(2)))
        for match in re.finditer(r"(?<![.\w])(\w+)\s*=\s*(\w+)\s*\(\s*\)", line):
            value = factory_id(match.group(2))
            if value:
                ids.setdefault(match.group(1), value)
    return ids


def entries(text: str, call: str) -> list[tuple[int, str]]:
    """(base meta, MT field) for every ``call(base, ... , MT.<field>)`` line."""
    return [
        (int(base), field)
        for base, field in re.findall(
            r"%s\s*\(\s*(\d+)\s*,[^;]*?\bMT\.(\w+)\s*\)\s*;" % call, text
        )
    ]


def signal_wires(text: str) -> list[tuple[int, str, str]]:
    """(meta, form, MT field) for the explicit redstone wire/cable registrations."""
    rows: list[tuple[int, str, str]] = []
    field = None
    for line in text.splitlines():
        declared = re.search(r"\baMat\s*=\s*MT\.(\w+)\s*;", line)
        if declared:
            field = declared.group(1)
            continue
        registered = re.search(
            r"aRegistry\.add\([^;]*?,\s*(\d+)\s*,\s*27050\s*,\s*"
            r"MultiTileEntityWireRedstone(Insulated)?\b",
            line,
        )
        if registered and field:
            form = "cableGt01" if registered.group(2) else "wireGt01"
            rows.append((int(registered.group(1)), form, field))
    return rows


def cells(text: str) -> list[tuple[int, str]]:
    rows: list[tuple[int, str]] = []
    for line in text.splitlines():
        field = re.search(r"\baMat\s*=\s*MT\.(\w+)\s*;", line)
        meta = re.search(
            r"aRegistry\.add\([^;]*?,\s*(\d{4,5})\s*,\s*\d{4,5}\s*,\s*MultiTileEntityCell\b", line
        )
        if field and meta:
            rows.append((int(meta.group(1)), field.group(1)))
    return rows


def build(source: str) -> dict[str, str]:
    loader = read_source(os.path.join(source, "gregtech", "loaders", "b", "Loader_MultiTileEntities.java"))
    electric = read_source(
        os.path.join(source, "gregapi", "tileentity", "connectors", "MultiTileEntityWireElectric.java")
    )
    item_pipes = read_source(
        os.path.join(source, "gregapi", "tileentity", "connectors", "MultiTileEntityPipeItem.java")
    )
    fluid_pipes = read_source(
        os.path.join(source, "gregapi", "tileentity", "connectors", "MultiTileEntityPipeFluid.java")
    )
    mt = read_source(os.path.join(source, "gregapi", "data", "MT.java"))
    ids = material_ids(mt)

    def resolve(field: str) -> int:
        if field in FIELD_ID_OVERRIDES:
            return FIELD_ID_OVERRIDES[field]
        if field not in ids:
            raise SystemExit("Unresolved MT field: " + field)
        return ids[field]

    metas: dict[str, str] = {}
    assert len(entries(loader, r"MultiTileEntityPipeFluid\.addFluidPipes")) == 40
    for base, field in entries(loader, r"MultiTileEntityPipeFluid\.addFluidPipes"):
        for offset, size in enumerate(FLUID_PIPE_SIZES):
            metas[str(base + offset)] = "pipe%s#%d" % (size, resolve(field))

    assert len(entries(loader, r"MultiTileEntityPipeItem\.addItemPipes")) == 21
    for base, field in entries(loader, r"MultiTileEntityPipeItem\.addItemPipes"):
        for offset, form in ITEM_PIPE_OFFSETS.items():
            metas[str(base + offset)] = "%s#%d" % (form, resolve(field))

    electric_bases = entries(loader, r"MultiTileEntityWireElectric\.addElectricWires")
    assert len(electric_bases) == 30
    # The cable line is gated by aCable; every registration in the reviewed source passes T.
    assert electric.count("aCable") >= 1
    for base, field in electric_bases:
        for offset in range(16):
            metas[str(base + offset)] = "wireGt%02d#%d" % (offset + 1, resolve(field))
        for offset, size in CABLE_OFFSETS.items():
            metas[str(base + offset)] = "cableGt%02d#%d" % (size, resolve(field))

    signals = signal_wires(loader)
    assert len(signals) == 6, "redstone wire registrations changed"
    for meta, form, field in signals:
        metas[str(meta)] = "%s#%d" % (form, resolve(field))

    cell_rows = cells(loader)
    assert len(cell_rows) == 40
    for meta, field in cell_rows:
        metas[str(meta)] = "capcellcon#%d" % resolve(field)

    return dict(sorted(metas.items(), key=lambda kv: int(kv[0])))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, help="GT6 checkout's src/main/java directory")
    parser.add_argument("--check", action="store_true", help="fail when the committed table differs")
    args = parser.parse_args()

    metas = build(args.source)
    text = json.dumps({"metas": metas}, indent=2, sort_keys=False) + "\n"
    kinds: dict[str, int] = {}
    for value in metas.values():
        kinds[value.split("#")[0].rstrip("0123456789")] = kinds.get(value.split("#")[0].rstrip("0123456789"), 0) + 1
    print("metas: %d (%s)" % (len(metas), ", ".join("%s=%d" % kv for kv in sorted(kinds.items()))))

    if args.check:
        try:
            current = open(OUT_JSON, encoding="utf-8").read()
        except FileNotFoundError:
            current = ""
        if current != text:
            print("gt6_multiblock_identities.json is stale; rerun without --check")
            sys.exit(1)
        print("table matches the source")
        return
    with open(OUT_JSON, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    print("wrote " + OUT_JSON)


if __name__ == "__main__":
    main()
