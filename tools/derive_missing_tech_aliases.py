"""Derive the aliases that turn GT6 "missing content" tokens into items the port already has.

``tools/transpile_gt6_chem.py`` spells every ``IL.<Name>.get(n)`` as ``tech:<snake_case name>``, but
the multi-item transpiler (``tools/transpile_gt6_multiitems.py``) derives its ids from the *display
name* GT6 passes to ``addItem`` — ``IL.Food_Butter`` becomes ``tech:food_butter`` while the port
registers ``butter``. This tool reads GT6's own registrations, applies the same snake_case rule and
reports which of those ids the port has, so the two spellings can be bridged with an alias entry.

  * ``MultiItem*.java``  -> ``addItem(id, "Display Name", ...)``      -> snake(display)
  * ``LoaderItemList.java`` -> ``IL.X.set(ST.make(Items.foo, 1, W))`` -> ``minecraft:foo``

Usage:  python tools/derive_missing_tech_aliases.py [--json docs/tech-item-token-aliases.json]
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
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
GENERATED = native_path(ROOT / "src/main/java/com/gregtech/gregtech")
TOKENS = ROOT / "docs/tech-item-tokens.json"
OUT = ROOT / "docs/tech-item-token-aliases.json"

ADD_ITEM = re.compile(r'IL\.(\w+)\s*\.set\(\s*addItem\(\s*\d+\s*,\s*"((?:\\.|[^"\\])*)"')
VANILLA = re.compile(r"IL\.(\w+)\s*\.set\(\s*ST\.make\(\s*(Items|Blocks)\.(\w+)\s*,\s*1\s*,\s*(\w+)")
SNAKE_JUNK = re.compile(r"[''.()&,!?]")

# 1.7.10 vanilla names that 1.20.1 spells differently (the port's own table lives in
# tools/generate_vanilla_compositions.py; these are the ones the missing tokens hit).
VANILLA_FIXES = {
    ("Items", "dye", "15"): "minecraft:bone_meal",
    ("Items", "wheat", "W"): "minecraft:wheat",
    ("Items", "hay_block", "W"): "minecraft:hay_block",
    ("Items", "potato", "1"): "minecraft:poisonous_potato",
}


def snake(name: str) -> str:
    text = SNAKE_JUNK.sub("", name.lower())
    return re.sub(r"[^a-z0-9]+", "_", text).strip("_")


def port_ids() -> set[str]:
    tech = (GENERATED / "registry/GTTechnological.java").read_text(encoding="utf-8")
    tech = re.sub(r"//[^\n]*", "", tech)
    block = tech.split("static final String[] IDS = {", 1)[1].split("};", 1)[0]
    ids = set(re.findall(r'"([a-z0-9_]+)"', block))
    entries = (GENERATED / "registry/GTMultiItemsGen.java").read_text(encoding="utf-8")
    entries = entries.split("static final String[] ENTRIES = {", 1)[1].split("};", 1)[0]
    # the tooltip flag column is "T", "F" or "" depending on the entry
    ids |= set(re.findall(r'"([a-z0-9_]+)",\s*"[^"]*",\s*"[a-z]+",\s*"[TF]?"', entries))
    return ids


def gt6_registrations() -> dict[str, str]:
    """``snake(IL field)`` -> the id the port would use for the same item.

    The token the transpiler emits is ``snake(field)`` (``Food_PotatoChips`` -> ``food_potatochips``)
    while the port's id comes from the display name (``"Potato Chips"`` -> ``potato_chips``), so the
    two are matched on the field spelling and bridged by the display-name id.
    """
    out: dict[str, str] = {}
    for path in GT6.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        for field, display in ADD_ITEM.findall(text):
            out.setdefault(snake(field), snake(display))
        for field, kind, name, meta in VANILLA.findall(text):
            fixed = VANILLA_FIXES.get((kind, name, meta))
            out.setdefault(snake(field), fixed or f"minecraft:{snake(name)}")
    return out


def main() -> None:
    wanted = json.loads(TOKENS.read_text(encoding="utf-8"))
    missing = [entry["id"] if isinstance(entry, dict) else entry for entry in wanted["missing"]]
    ids = port_ids()
    registry = gt6_registrations()

    resolvable: dict[str, dict[str, str]] = {}
    unresolved: list[str] = []
    for token in missing:
        target = registry.get(token)
        if target and (target in ids or target.startswith("minecraft:")):
            resolvable[token] = {"gt6Field": token, "portId": target}
        else:
            unresolved.append(token)

    report = {
        "referencedMissing": len(missing),
        "aliasable": resolvable,
        "stillMissing": unresolved,
    }
    OUT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(missing)} missing tokens; {len(resolvable)} map onto an item the port has, "
          f"{len(unresolved)} really have no counterpart")
    for token, data in sorted(resolvable.items()):
        print(f"  {token:28} -> {data['portId']:34} (snake of IL.{data['gt6Field']})")
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
