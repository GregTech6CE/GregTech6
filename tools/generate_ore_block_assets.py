#!/usr/bin/env python3
"""Generate blockstate/block model/item JSONs for GT ore and small-ore blocks.

Block ids are ``ore[_small]_<sanitize(materialName).lower()>``: ``BlockMaterialPrefix#getBlockId``
builds ``registryName + "_" + material.getName().toLowerCase()``, where ``registryName`` is
``MaterialPrefix.camelToSnake`` of the prefix name (``ore`` / ``ore_small``) and ``GTMaterial``
sanitizes the declaration name in its constructor.  So the ore materials are exactly the materials
``MaterialPrefixes`` accepts the two block prefixes for:

    m.isValid() && !m.has(HIDDEN) && (m.has(GENERATE_ORE) || MaterialForms.has(m, "ORES"))

This generator collects that set from every source that can satisfy the second half:

* ``MaterialForms`` rows (GT6's form sets, ``TD.java``) whose flags contain ``ORES`` — matched by the
  GT6 material id first and by the normalised material name second, exactly like ``MaterialForms#of``;
* declarations through the ``ore(...)`` factory — ``MaterialFactories#ore`` is what puts
  ``GENERATE_ORE`` on a material the port declares itself;
* the identifier references of the worldgen tables, which ``GTWorldgenMaterials#flagOreMaterials``
  resolves before item/block registration (``GTOreVeins`` vein top/bottom/between/spread, the
  small-ore and bedrock-ore tables, and ``GTStoneLayersGen``'s layer/contact ore lists);
* the *string* references of those same tables: ``GTStoneLayersGen`` names its ores by string
  (``ore("FluoriteWhite", true, 16, 0, 48)``) and flags them through ``GTMaterialRegistry.get(name)``,
  so the literals passed to ``ore(...)`` are a material list of their own that no identifier scan sees;
* every ore asset that is already on disk, so a re-run never deletes a shipped file (the first pass
  generated GT6's stone-ore ids such as ``ore_basalt``/``ore_marble``, which the rules above no longer
  select).

The four source rules alone select 485 materials — exactly the ``ore blocks=970`` the gate's
``§103.B`` gametest counts (970 / 2 prefixes), against the 242 the previous revision covered — and the
shipped-asset union grows that to 519 materials (1038 blocks).  The surplus ids are never registered,
so their files stay inert.

The JSON models are placeholders (the real rendering is the runtime ``OreBakedModel`` with the
NBT-driven stone background); they exist so model loading does not log missing-model errors for
every ore block.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from modular_materials import parse_catalog

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
BLOCKSTATES = SHARED_ASSETS / "blockstates"
MODELS = SHARED_ASSETS / "models/block/ores"
ITEM_MODELS = SHARED_ASSETS / "models/item"

JAVA = ROOT / "src/main/java/com/gregtech/gregtech"
GEN_MATERIALS = ROOT / "tools/generated/materials-source.java.txt"
MT_FILE = JAVA / "data/ImportedMaterialData.java"
FORM_FILE = JAVA / "data/generated/MaterialForms.java"
WORLDGEN_FILES = [
    JAVA / "worldgen/GTOreVeins.java",
    JAVA / "worldgen/GTStoneLayersGen.java",
]

# Field = factory(id, "Name", ...)
FACTORY_LINE = re.compile(r"(\w+)\s*=\s*(\w+)\(\s*(\d+)\s*,\s*\"([^\"]+)\"")
# Field = GT6Materials.Nested.Other  (alias)
ALIAS_LINE = re.compile(r"(\w+)\s*=\s*GT6Materials\.(?:\w+)\.(\w+)\s*[,;]")
# "id|FLAG,FLAG,..." row of MaterialForms.DATA
FORM_ROW = re.compile(r'^\s*"([^"|]+)\|([A-Z_,]*)"', re.M)
# MT.OREMATS.X / MT.STONES.X / Materials.X / OreMaterials.X references in the worldgen tables
WORLDGEN_REF = re.compile(
    r"\b(?:MT|Materials|ElementMaterials|CompoundMaterials|OreMaterials|StoneMaterials|WoodMaterials)"
    r"\.(?:OREMATS\.|STONES\.|WOODS\.)?(\w+)")
# GTStoneLayersGen names its ores by string and flags them through GTMaterialRegistry.get(name).
WORLDGEN_ORE_NAME = re.compile(r'ore\(\s*"([^"]+)"')

# Source labels of the selection report, in evaluation order.
SOURCE_FORMS = "MaterialForms ORES row"
SOURCE_FACTORY = "ore() factory (GENERATE_ORE)"
SOURCE_WORLDGEN_REF = "worldgen material reference"
SOURCE_WORLDGEN_NAME = "worldgen ore() name"
SOURCE_SHIPPED = "already shipped asset"


def sanitize(name: str) -> str:
    """GTMaterial.sanitize + lowercase: registry-id form of a material name."""
    return name.replace(" ", "").replace("-", "").replace("'", "").lower()


def normalize(name: str) -> str:
    """MaterialForms.normalize: lowercase, everything but [a-z0-9] dropped."""
    return "".join(c for c in name.lower() if "a" <= c <= "z" or "0" <= c <= "9")


def parse_form_flags() -> dict[str, set[str]]:
    """MaterialForms.DATA as ``GT6 material id or normalised name -> form flags``."""
    flags: dict[str, set[str]] = {}
    text = FORM_FILE.read_text(encoding="utf-8")
    for key, row in FORM_ROW.findall(text):
        flags[key] = set(row.split(","))
    return flags


def parse_definitions() -> tuple[dict[str, set[str]], list[tuple[str, str, int, str]]]:
    """Returns (reference token -> declared material names, ``(field, factory, id, name)`` rows).

    A token is a legacy GT6 field name (``MT.H``), a readable catalogue name (``Materials.Hydrogen``)
    or an alias to either; every interpretation is kept so a worldgen reference resolves whatever
    naming style the table used.  The factory of each declaration says whether the material carries
    ``GENERATE_ORE`` (``ore(...)``) or has to prove it through its ``MaterialForms`` row.
    """
    tokens: dict[str, set[str]] = {}
    declarations: list[tuple[str, str, int, str]] = []

    def add(token: str, name: str) -> None:
        tokens.setdefault(token, set()).add(name)

    for path in (GEN_MATERIALS, MT_FILE):
        text = path.read_text(encoding="utf-8")
        for field, factory, raw_id, name in FACTORY_LINE.findall(text):
            declarations.append((field, factory, int(raw_id), name))
            add(field, name)
    # one level of aliasing (MT.OREMATS.X = GT6Materials.Ores.X)
    alias_text = MT_FILE.read_text(encoding="utf-8")
    for field, target in ALIAS_LINE.findall(alias_text):
        if field not in tokens and target in tokens:
            tokens[field] = set(tokens[target])
    # The readable catalogues name their fields after the material, so `Materials.FluoriteWhite` has
    # to resolve next to the legacy `MT.FluoriteWhite`.
    for rows in parse_catalog(GEN_MATERIALS.read_text(encoding="utf-8")).values():
        for old, readable, expression in rows:
            match = FACTORY_LINE.search(old + " = " + expression)
            if match:
                add(readable, match.group(4))
    return tokens, declarations


def shipped_material_ids() -> set[str]:
    """Ore material ids the asset directories already carry (``ore[_small]_<id>.json``).

    Only plain lowercase/alphanumeric material ids are accepted, so a file of another
    ``ore``-looking prefix (GT6's raw ore items are ``ore_raw_<material>``) is never mistaken for an
    ore block asset.
    """
    ids: set[str] = set()
    for directory in (BLOCKSTATES, MODELS, ITEM_MODELS):
        for path in directory.glob("ore_*.json"):
            stem = path.stem
            material_id = stem[len("ore_small_"):] if stem.startswith("ore_small_") else stem[len("ore_"):]
            if re.fullmatch(r"[a-z0-9]+", material_id):
                ids.add(material_id)
    return ids


def write_json(path: Path, data: dict) -> bool:
    path.parent.mkdir(parents=True, exist_ok=True)
    body = json.dumps(data, indent=2) + "\n"
    if path.exists() and path.read_text(encoding="utf-8") == body:
        return False
    path.write_text(body, encoding="utf-8")
    return True


def main() -> int:
    tokens, declarations = parse_definitions()
    form_flags = parse_form_flags()

    selected: dict[str, set[str]] = {}

    def select(material_id: str, source: str) -> None:
        selected.setdefault(material_id, set()).add(source)

    # The ``ore(...)`` factory declarations and GT6's own form table: the two halves of the
    # BlockMaterialPrefix condition that the port can answer from its own sources.
    for _field, factory, raw_id, name in declarations:
        flags = form_flags.get(str(raw_id)) or form_flags.get(normalize(name)) or set()
        if "ORES" in flags:
            select(sanitize(name), SOURCE_FORMS)
        if factory == "ore":
            select(sanitize(name), SOURCE_FACTORY)

    # The worldgen tables GTWorldgenMaterials#flagOreMaterials walks.  Identifier references resolve
    # to the material they name; the ore() literals are names already (GTMaterialRegistry.get).
    unresolved: list[str] = []
    for path in WORLDGEN_FILES:
        text = path.read_text(encoding="utf-8")
        for token in WORLDGEN_REF.findall(text):
            names = tokens.get(token)
            if names is None:
                unresolved.append(token)
                continue
            for name in names:
                select(sanitize(name), SOURCE_WORLDGEN_REF)
        for name in WORLDGEN_ORE_NAME.findall(text):
            select(sanitize(name), SOURCE_WORLDGEN_NAME)
    if unresolved:
        print(f"WARNING: worldgen references with no material definition: {sorted(set(unresolved))}")

    # Keep whatever a previous pass shipped: an id the rules above no longer select may still be
    # referenced by a block, and deleting its files is the one mistake this generator must not make.
    shipped = shipped_material_ids()
    for material_id in shipped:
        select(material_id, SOURCE_SHIPPED)

    ids = sorted(selected)
    print(f"Selected {len(ids)} ore materials ({len(ids) * 2} ore blocks)")
    for source in (SOURCE_FORMS, SOURCE_FACTORY, SOURCE_WORLDGEN_REF, SOURCE_WORLDGEN_NAME,
                   SOURCE_SHIPPED):
        count = sum(1 for sources in selected.values() if source in sources)
        print(f"  {count:4d}  from {source}")
    print(f"  {sum(1 for material_id in ids if material_id in shipped):4d}  already on disk"
          f" ({len(shipped)} shipped, {len(shipped - set(ids))} of them stale)")

    # Remove stale ore assets (old files were named after snake-cased field names).  With the
    # shipped-asset source above this is a no-op unless a file was added by hand.
    removed = 0
    valid_names = {f"{prefix}_{mat}" for prefix in ("ore", "ore_small") for mat in ids}
    for directory in (BLOCKSTATES, MODELS, ITEM_MODELS):
        for f in directory.glob("ore_*.json"):
            if f.stem not in valid_names:
                f.unlink()
                removed += 1
    if removed:
        print(f"Removed {removed} stale ore asset files")

    created = 0
    for prefix in ("ore", "ore_small"):
        for mat in ids:
            created += write_json(BLOCKSTATES / f"{prefix}_{mat}.json", {
                "variants": {"": {"model": f"gregtech:block/ores/{prefix}_{mat}"}}
            })
            created += write_json(MODELS / f"{prefix}_{mat}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": "minecraft:block/stone"}
            })
            created += write_json(ITEM_MODELS / f"{prefix}_{mat}.json", {
                "parent": f"gregtech:block/ores/{prefix}_{mat}"
            })

    print(f"Wrote {created} ore block asset files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
