"""Audit the shipped GT6CE datapack recipe tree (stdlib only).

Usage:
    python tools/recipes/audit_recipes.py [--report PATH] [--converted DIR] [--strict]

Checks:
  * JSON parse failures and unknown recipe types
  * shaped pattern/key consistency and result shape
  * shapeless and smelting field shape
  * gt6ce: tag references resolve to a tag file (including nested values)
  * concrete circuit-family ingredients where a cumulative tier tag is expected
  * ambiguous shaped recipes (same type+pattern+key, different results)
  * unknown top-level fields (typo guard)
  * static publish exclusions exist and name a shipped recipe
  * `--converted DIR`: one native output per published recipe and item tag, no stale or
    legacy-shaped file, and every converted tag keeps its source values
  * gt6ce: item ids that have no shipped, table, family, procedural or placeholder model or
    blockstate (warning only)

Exit code is 0 unless --strict is given and errors were found.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
RECIPE_DIR = Path("core/src/main/resources/data/gt6ce/recipes")
SHARED_DATA = Path("core/src/main/resources/data")
DATA_ROOTS = [
    Path("core/src/main/resources/data"),
    Path("minecraft/versions/1.20.1-forge/src/main/resources/data"),
    Path("minecraft/versions/1.21.1-neoforge/src/main/resources/data"),
]
ASSET_ROOTS = [
    Path("core/src/main/resources"),
    Path("minecraft/src/main/resources"),
]

# Virtual model and blockstate tables served by SharedInventoryModelPack.
MODEL_DEFINITIONS = Path("core/src/main/resources/assets/gt6ce/model_definitions.tsv")
BLOCKSTATE_DEFINITIONS = Path("core/src/main/resources/assets/gt6ce/blockstate_definitions.tsv")

# Java tables that drive the models and blockstates ProceduralModels renders at runtime.
PROCEDURAL_MODELS = Path("core/src/main/java/gregtech/engine/resource/ProceduralModels.java")
STONE_VARIANTS = Path("core/src/main/java/gregtech/content/block/stone/StoneVariant.java")
BATTERY_SPECS = Path("core/src/main/java/gregtech/content/energy/ChemicalBatterySpec.java")

# Java catalogue tables that drive the blockstate families FamilyBlockstateGenerators renders.
FAMILY_BLOCKSTATES = Path("core/src/main/java/gregtech/engine/resource/FamilyBlockstateGenerators.java")
SENSOR_CATALOG = Path("core/src/main/java/gregtech/engine/sensor/SensorCatalog.java")
TRACK_CATALOG = Path("core/src/main/java/gregtech/content/transport/TrackCatalog.java")
WOOD_SLAB_CATALOG = Path("core/src/main/java/gregtech/content/plant/WoodSlabCatalog.java")

# Java tables that drive the client pack placeholders for smeltery components and hoppers.
SMELTERY_DEFINITIONS = Path("core/src/main/java/gregtech/engine/machine/OriginalSmelteryDefinitions.java")
HOPPER_CATALOG = Path("core/src/main/java/gregtech/content/transport/HopperCatalog.java")

# One component family per crucible tier; `mold_` must not swallow the `mold_basin_` rows.
SMELTERY_COMPONENT_PREFIXES = (
    "smelting_crucible_",
    "mold_",
    "mold_basin_",
    "crucible_crossing_",
    "crucible_faucet_",
)

KNOWN_TYPES = {
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "minecraft:smelting",
    "gt6ce:tool_shaped",
    "gt6ce:tool_shapeless",
    "gt6ce:book_binding",
    "gt6ce:spike_conversion",
    "gt6ce:tape_repack",
    "gt6ce:tool_repair",
}

KNOWN_FIELDS = {
    "type",
    "result",
    "ingredients",
    "pattern",
    "key",
    "ingredient",
    "experience",
    "cookingtime",
    "category",
    "group",
    "allow_mirror",
    "construction_color",
    "require_empty_fluid_containers",
    "gregtech_autocraftable",
    "gregtech_form_variants",
    "gregtech_form_offset",
}

DOC_FIELDS = {"_comment", "_mirror_flags", "_autocrafting_source"}

SHAPED_TYPES = {"minecraft:crafting_shaped", "gt6ce:tool_shaped"}
SHAPELESS_TYPES = {"minecraft:crafting_shapeless", "gt6ce:tool_shapeless", "gt6ce:tape_repack"}

# Items that stand in for GT6's cumulative OD_CIRCUITS[n] and must be tags in recipes.
CIRCUIT_FAMILY = re.compile(
    r"^gt6ce:(circuit_(basic|good|advanced|elite|master|ultimate)"
    r"|integrated_circuit_\d+"
    r"|crystal_circuit_[a-z0-9_]+)$"
)

ID_PATTERN = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def rel(path: Path) -> str:
    return path.relative_to(REPO).as_posix()


def load_recipes():
    recipes = {}
    errors = []
    for path in sorted((REPO / RECIPE_DIR).rglob("*.json")):
        try:
            recipes[path] = json.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report and continue
            errors.append({"kind": "json", "file": rel(path), "detail": str(exc)})
    return recipes, errors


def tag_ids():
    """Static tags from the shared tree and both version-specific trees."""
    tags = set()
    for data in DATA_ROOTS:
        base = REPO / data
        if not base.is_dir():
            continue
        for kind in ("items", "item"):
            for namespace in base.glob("*/tags/" + kind):
                ns = namespace.parent.parent.name
                for path in namespace.rglob("*.json"):
                    tags.add(f"{ns}:" + path.relative_to(namespace).with_suffix("").as_posix())
    return tags


def model_ids():
    """Item ids with a shipped or generated model or blockstate."""
    ids = set()
    for root in ASSET_ROOTS:
        for namespace in (REPO / root / "assets").glob("*"):
            models = namespace / "models" / "item"
            if models.is_dir():
                for path in models.rglob("*.json"):
                    ids.add(
                        f"{namespace.name}:{path.relative_to(models).with_suffix('').as_posix()}"
                    )
            states = namespace / "blockstates"
            if states.is_dir():
                for path in states.rglob("*.json"):
                    ids.add(f"{namespace.name}:{path.relative_to(states).with_suffix('').as_posix()}")
    return ids | virtual_model_ids()


def definition_rows(relative: Path, columns: int):
    """Non-comment, tab-separated rows of a virtual definition table."""
    path = REPO / relative
    if not path.is_file():
        raise RuntimeError(f"missing virtual definition table: {relative.as_posix()}")
    rows = []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        row = line.split("\t")
        if len(row) != columns:
            raise RuntimeError(
                f"{relative.as_posix()}:{number}: expected {columns} columns, got {len(row)}"
            )
        rows.append(row)
    if not rows:
        raise RuntimeError(f"no rows in virtual definition table: {relative.as_posix()}")
    return rows


def java_section(text: str, marker: str, delimiters: str = "()"):
    """Text inside the first delimiter pair that follows `marker`."""
    start = text.find(marker)
    if start < 0:
        raise RuntimeError(f"Java source no longer declares {marker!r}")
    opening = text.find(delimiters[0], start + len(marker))
    if opening < 0:
        raise RuntimeError(f"Java declaration {marker!r} has no {delimiters} body")
    depth = 0
    for index in range(opening, len(text)):
        char = text[index]
        if char == delimiters[0]:
            depth += 1
        elif char == delimiters[1]:
            depth -= 1
            if depth == 0:
                return text[opening + 1 : index]
    raise RuntimeError(f"unbalanced {delimiters} after {marker!r}")


def java_source(relative: Path) -> str:
    path = REPO / relative
    if not path.is_file():
        raise RuntimeError(f"missing Java source: {relative.as_posix()}")
    return path.read_text(encoding="utf-8")


def java_strings(block: str):
    return re.findall(r'"([^"]*)"', block)


def require_entries(values, family: str):
    """Rejects an empty extraction so a renamed table fails the audit instead of passing it."""
    if not values:
        raise RuntimeError(f"no entries extracted for {family}")
    return values


def require_unique(values, family: str):
    """Rejects a table that lists one id twice, which would collapse into a single model."""
    duplicates = sorted(value for value, count in Counter(values).items() if count > 1)
    if duplicates:
        raise RuntimeError(f"duplicate entries in {family}: {duplicates}")
    return values


def procedural_model_ids():
    """Item ids whose models and blockstates ProceduralModels renders from its family tables.

    The renderer tables are the source of truth, so a family that stops being emitted or a table
    that loses its entries fails the audit instead of silently dropping the check.
    """
    text = java_source(PROCEDURAL_MODELS)
    ids = set()

    stones = require_entries(
        re.findall(r'new StoneSpec\(\s*"([a-z0-9_]+)"', java_section(text, "STONE_SPECS")),
        "STONE_SPECS",
    )
    for stem in require_entries(
        java_strings(java_section(text, "private static void forEachPileBlockstateStem", "{}")),
        "forEachPileBlockstateStem",
    ):
        ids.add(f"gt6ce:{stem}")
    filter_literals = require_entries(
        java_strings(java_section(text, "FILTER_BLOCKSTATES")), "FILTER_BLOCKSTATES"
    )
    if len(filter_literals) % 2 != 0:
        raise RuntimeError(f"odd number of string literals in FILTER_BLOCKSTATES: {len(filter_literals)}")
    filters = filter_literals[::2]
    if any(not stem.startswith("filter_") for stem in filters):
        raise RuntimeError(f"unexpected FILTER_BLOCKSTATES key(s): {filters!r}")
    for stem in filters:
        ids.add(f"gt6ce:{stem}")
    extenders = require_entries(
        java_strings(java_section(text, "EXTENDER_BLOCKSTATES")), "EXTENDER_BLOCKSTATES"
    )
    for stem in extenders:
        ids.add(f"gt6ce:{stem}")
    modes = require_entries(java_strings(java_section(text, "COMPASS_MODES")), "COMPASS_MODES")
    frames = len(
        require_entries(java_strings(java_section(text, "COMPASS_DIRECTIONS")), "COMPASS_DIRECTIONS")
    )
    for mode in modes:
        ids.add(f"gt6ce:compass_{mode}")
        for frame in range(1, frames + 1):
            ids.add(f"gt6ce:compass/{mode}_{frame}")
    for size in require_entries(java_strings(java_section(text, "REACTOR_SIZES")), "REACTOR_SIZES"):
        if size == "1x1":
            ids.add("gt6ce:reactor_core")
        elif size == "2x2":
            ids.add("gt6ce:reactor_core_2x2")
        else:
            raise RuntimeError(f"unknown reactor core size: {size}")

    variants = require_entries(
        re.findall(
            r'^\s*[A-Z][A-Z0-9_]*\(\s*\d+\s*,\s*"([a-z0-9_]+)"',
            java_section(java_source(STONE_VARIANTS), "public enum StoneVariant", "{}"),
            re.MULTILINE,
        ),
        "StoneVariant",
    )
    for stone in stones:
        for variant in variants:
            base = f"{stone}_{variant}"
            ids.add(f"gt6ce:{base}")
            ids.add(f"gt6ce:{base}_slab")

    battery = java_source(BATTERY_SPECS)
    chemistries = require_entries(
        re.findall(
            r'^\s*[A-Z][A-Z0-9_]*\(\s*"([a-z0-9_]+)"',
            java_section(battery, "public enum Chemistry", "{}"),
            re.MULTILINE,
        ),
        "Chemistry",
    )
    tier_array = re.search(
        r"new String\[\]\s*\{([^}]*)\}", java_section(battery, "public String id()", "{}")
    )
    if tier_array is None:
        raise RuntimeError("ChemicalBatterySpec.id() no longer lists its tier names")
    tiers = require_entries(java_strings(tier_array.group(1)), "battery tiers")
    for chemistry in chemistries:
        for tier in tiers:
            ids.add(f"gt6ce:battery_{chemistry}_{tier}")
    return ids


def stem_family(prefix: str, materials):
    """One `<prefix>_<material>` stem per material of a family table."""
    return [f"{prefix}_{material}" for material in materials]


def table_strings(text: str, name: str):
    """String literals of one family table, declared as a `List.of`."""
    return require_entries(java_strings(java_section(text, name)), name)


def family_blockstate_ids():
    """Item ids of the blockstate stems FamilyBlockstateGenerators renders from its family tables.

    The generator tables are the source of truth, so a family that stops being emitted or a table
    that loses its entries fails the audit instead of silently dropping the check.
    """
    text = java_source(FAMILY_BLOCKSTATES)
    storage = table_strings(text, "STORAGE_MATERIALS")
    if "withoutPlainSteel(STORAGE_MATERIALS)" not in text:
        raise RuntimeError("MECHANICAL_MATERIALS no longer derives from STORAGE_MATERIALS")
    mechanical = [material for material in storage if material != "steel"]
    if len(mechanical) == len(storage):
        raise RuntimeError("STORAGE_MATERIALS no longer lists the plain steel entry")

    stems = set()
    for prefix in ("safe", "scaffold"):
        stems.add(prefix)
        stems.update(stem_family(prefix, mechanical))
    stems.update(f"key_safe_{material}" for material in storage)
    for prefix, materials in require_entries(
        re.findall(r'new EngineFamily\(\s*"([a-z0-9_]+)",\s*"[a-z0-9_]+",\s*([A-Z_]+)\)', text),
        "EngineFamily",
    ):
        stems.update(stem_family(prefix, table_strings(text, materials)))
    for prefix, materials in require_entries(
        re.findall(
            r'new FluidAttachment\(\s*"([a-z0-9_]+)",\s*"[a-z0-9_]+",\s*"[a-z0-9_]+",\s*([A-Z_]+)\)',
            text,
        ),
        "FluidAttachment",
    ):
        stems.add(prefix)
        stems.update(stem_family(prefix, table_strings(text, materials)))
    stems.update(
        require_entries(
            re.findall(
                r'new Entry\("([a-z0-9_]+)"',
                java_section(java_source(SENSOR_CATALOG), "public static final List<Entry> ALL"),
            ),
            "SensorCatalog.ALL",
        )
    )
    for name, tiers in require_entries(
        re.findall(r'new EnergyMachine\(\s*"([a-z0-9_]+)",\s*([A-Z_]+)\)', text), "EnergyMachine"
    ):
        stems.update(stem_family(name, table_strings(text, tiers)))
    track_kinds = table_strings(text, "TRACK_KINDS")
    for material in require_entries(
        re.findall(
            r'new Material\("([A-Za-z]+)"',
            java_section(
                java_source(TRACK_CATALOG), "public static final List<Material> MATERIALS"
            ),
        ),
        "TrackCatalog.MATERIALS",
    ):
        slug = material.lower()
        stems.update(f"{kind}_{slug}" for kind in track_kinds)
    for wood in require_entries(
        java_strings(
            java_section(
                java_source(WOOD_SLAB_CATALOG), "public static final java.util.List<String> WOODS"
            )
        ),
        "WoodSlabCatalog.WOODS",
    ):
        stems.add(f"slab_{wood}")
    return {f"gt6ce:{stem}" for stem in stems}


def smeltery_component_family(stem: str) -> str:
    """Longest smeltery component prefix of `stem`, so `mold_basin_` wins over `mold_`."""
    matches = [prefix for prefix in SMELTERY_COMPONENT_PREFIXES if stem.startswith(prefix)]
    if not matches:
        raise RuntimeError(f"unexpected smeltery component id: {stem}")
    return max(matches, key=len)


def placeholder_model_ids():
    """Item ids whose models and blockstates the client pack renders from placeholder templates.

    The smeltery and hopper tables are the source of truth, so a table that loses entries or a
    component family that stops being emitted fails the audit instead of silently dropping the
    check.
    """
    smeltery = require_unique(
        require_entries(
            re.findall(r'add\(\s*"([a-z0-9_]+)"', java_source(SMELTERY_DEFINITIONS)),
            "OriginalSmelteryDefinitions",
        ),
        "OriginalSmelteryDefinitions",
    )
    families = Counter(smeltery_component_family(stem) for stem in smeltery)
    if set(families) != set(SMELTERY_COMPONENT_PREFIXES):
        raise RuntimeError(f"smeltery component families changed: {sorted(families)}")
    sizes = set(families.values())
    if len(sizes) != 1 or 0 in sizes:
        raise RuntimeError(f"uneven smeltery component families: {dict(sorted(families.items()))}")

    hoppers = require_unique(
        require_entries(
            re.findall(
                r'new Entry\(\s*(true|false),\s*HopperSpec\.of\(\s*"([a-z0-9_]+)"',
                java_source(HOPPER_CATALOG),
            ),
            "HopperCatalog.ALL",
        ),
        "HopperCatalog.ALL",
    )
    regular = [stem for queue, stem in hoppers if queue == "false"]
    queued = [stem for queue, stem in hoppers if queue == "true"]
    if not regular or not queued:
        raise RuntimeError(f"hopper catalog lost its regular or queue entries: {len(hoppers)}")
    for stems, prefix in ((regular, "hopper_"), (queued, "queue_hopper_")):
        unexpected = [stem for stem in stems if not stem.startswith(prefix)]
        if unexpected:
            raise RuntimeError(f"unexpected {prefix}* id(s): {unexpected}")

    return {"gt6ce:" + stem for stem in smeltery + regular + queued}


def virtual_model_ids():
    """Item ids whose models and blockstates are generated instead of shipped as JSON."""
    models = {
        "gt6ce:" + row[0][len("item/") :]
        for row in definition_rows(MODEL_DEFINITIONS, 4)
        if row[0].startswith("item/")
    }
    if not models:
        raise RuntimeError(f"no item rows in {MODEL_DEFINITIONS.as_posix()}")
    states = {"gt6ce:" + row[0] for row in definition_rows(BLOCKSTATE_DEFINITIONS, 3)}
    return models | states | procedural_model_ids() | family_blockstate_ids() | placeholder_model_ids()


def camel_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def material_prefix_names():
    """Item/block prefix registry names; their models are generated at runtime."""
    names = set()
    prefixes = REPO / "core/src/main/java/gregtech/content/data/MaterialPrefixes.java"
    text = prefixes.read_text(encoding="utf-8")
    for name in re.findall(r'register\("([A-Za-z0-9_]+)"\)', text):
        names.add(camel_to_snake(name))
    for name in re.findall(r'block\("([A-Za-z0-9_]+)"\)', text):
        names.add(camel_to_snake(name))
    enum = REPO / "core/src/main/java/gregtech/content/data/MaterialPrefix.java"
    for name in re.findall(r'def\("([A-Za-z0-9_]+)"', enum.read_text(encoding="utf-8")):
        names.add(camel_to_snake(name))
    return names


def runtime_tag_families():
    """Tag families built by the runtime in-memory packs (MaterialTagPack et al.).

    Static tag files cannot list these; the families come from the code that emits
    them. A missing tag whose first segment is not a known family is a real defect.
    """
    families = set()
    prefixes = REPO / "core/src/main/java/gregtech/content/data/MaterialPrefixes.java"
    prefix_text = prefixes.read_text(encoding="utf-8")
    for name in re.findall(r'register\("([A-Za-z0-9_]+)"\)', prefix_text):
        snake = camel_to_snake(name)
        families.add(snake)
        families.add(snake + "s")
    for name in re.findall(r'block\("([A-Za-z0-9_]+)"\)', prefix_text):
        families.add(camel_to_snake(name))
    prefix_enum = REPO / "core/src/main/java/gregtech/content/data/MaterialPrefix.java"
    for name in re.findall(r'def\("([A-Za-z0-9_]+)"', prefix_enum.read_text(encoding="utf-8")):
        snake = camel_to_snake(name)
        families.add(snake)
        families.add(snake + "s")
    pack = REPO / "minecraft/src/main/java/gregtech/content/data/MaterialTagPack.java"
    text = pack.read_text(encoding="utf-8")
    families.update(re.findall(r"tags/items/([a-z0-9_]+)", text))
    families.update(re.findall(r'add\(tags,"gt6ce","items","([a-z0-9_]+)', text))
    return families


def ingredients(entry, where, errors):
    if isinstance(entry, list):
        refs = []
        for nested in entry:
            refs.extend(ingredients(nested, where, errors))
        return refs
    if not isinstance(entry, dict):
        errors.append({"kind": "ingredient", "file": where, "detail": f"not an object: {entry!r}"})
        return []
    if "item" in entry:
        if not isinstance(entry["item"], str) or not ID_PATTERN.match(entry["item"]):
            errors.append(
                {"kind": "ingredient", "file": where, "detail": f"bad item id: {entry['item']!r}"}
            )
        return []
    if "tag" in entry:
        if not isinstance(entry["tag"], str):
            errors.append(
                {"kind": "ingredient", "file": where, "detail": f"bad tag id: {entry['tag']!r}"}
            )
        return [entry["tag"]]
    if "type" in entry:
        return []
    errors.append(
        {"kind": "ingredient", "file": where, "detail": f"missing item/tag/type: {entry!r}"}
    )
    return []


def check_shaped(recipe, where, errors, tag_refs):
    pattern = recipe.get("pattern")
    key = recipe.get("key")
    if not isinstance(pattern, list) or not pattern or len(pattern) > 3:
        errors.append({"kind": "shape", "file": where, "detail": f"bad pattern: {pattern!r}"})
        return
    if any(not isinstance(row, str) or len(row) != len(pattern[0]) for row in pattern):
        errors.append({"kind": "shape", "file": where, "detail": f"ragged pattern: {pattern!r}"})
    if not isinstance(key, dict):
        errors.append({"kind": "shape", "file": where, "detail": f"bad key: {key!r}"})
        return
    for char in "".join(pattern):
        if char != " " and char not in key:
            errors.append(
                {"kind": "shape", "file": where, "detail": f"undefined key char {char!r}"}
            )
    for char, entry in key.items():
        if len(char) != 1:
            errors.append({"kind": "shape", "file": where, "detail": f"bad key char {char!r}"})
        tag_refs.extend(ingredients(entry, where, errors))


def check_shapeless(recipe, where, errors, tag_refs):
    entries = recipe.get("ingredients")
    if not isinstance(entries, list) or not entries:
        errors.append({"kind": "shape", "file": where, "detail": f"bad ingredients: {entries!r}"})
        return
    for entry in entries:
        tag_refs.extend(ingredients(entry, where, errors))


def check_result(recipe, where, errors):
    if recipe.get("type") == "gt6ce:tool_repair":
        return  # custom recipe: result depends on the repaired tool, no static result
    result = recipe.get("result")
    if isinstance(result, str):  # vanilla shorthand: "minecraft:green_dye"
        if not ID_PATTERN.match(result):
            errors.append({"kind": "result", "file": where, "detail": f"bad result: {result!r}"})
        return
    if not isinstance(result, dict):
        errors.append({"kind": "result", "file": where, "detail": f"bad result: {result!r}"})
        return
    if not isinstance(result.get("item"), str) or not ID_PATTERN.match(result["item"]):
        errors.append({"kind": "result", "file": where, "detail": f"bad result item: {result!r}"})
    count = result.get("count", 1)
    if not isinstance(count, int) or count < 1:
        errors.append({"kind": "result", "file": where, "detail": f"bad result count: {count!r}"})


def recipe_signature(recipe):
    try:
        key = json.dumps(recipe.get("key"), sort_keys=True)
    except TypeError:
        key = repr(recipe.get("key"))
    return (recipe.get("type"), tuple(recipe.get("pattern") or ()), key)


# The only static rows the NeoForge build does not publish: each is replaced by a runtime row.
EXCLUSIONS = Path("tools/recipes/static-publish-exclusions.txt")


def load_exclusions():
    """data-relative source paths that the 1.21.1 build replaces with generated rows."""
    entries = set()
    for line in (REPO / EXCLUSIONS).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            entries.add(line)
    return entries


def shared_tags():
    """Source tag files under the shared 1.20.1 tree, keyed by their native 1.21.1 path."""
    tags = {}
    for namespace in sorted((REPO / SHARED_DATA).iterdir()):
        tag_dir = namespace / "tags" / "items"
        if not tag_dir.is_dir():
            continue
        for path in sorted(tag_dir.rglob("*.json")):
            key = f"{namespace.name}/tags/item/{path.relative_to(tag_dir).as_posix()}"
            tags[key] = path
    return tags


def check_converted_tags(data: Path, errors):
    """Every shared tag reaches the native tree unchanged, and nothing else appears."""
    expected = shared_tags()
    for relative, source in sorted(expected.items()):
        target = data / relative
        if not target.is_file():
            errors.append({"kind": "converted-tag", "file": relative, "detail": "missing converted tag"})
            continue
        try:
            source_document = json.loads(source.read_text(encoding="utf-8-sig"))
            converted_document = json.loads(target.read_text(encoding="utf-8-sig"))
        except Exception as exc:  # noqa: BLE001 - report and continue
            errors.append({"kind": "converted-tag", "file": relative, "detail": str(exc)})
            continue
        if converted_document != source_document:
            errors.append(
                {"kind": "converted-tag", "file": relative, "detail": "values differ from source"}
            )
    actual = {
        path.relative_to(data).as_posix()
        for namespace in data.glob("*/tags/item")
        for path in namespace.rglob("*.json")
    }
    for extra in sorted(actual - set(expected)):
        errors.append({"kind": "converted-tag", "file": extra, "detail": "no matching source tag"})


def check_converted(root: Path, published):
    """Cross-check the build output: one native recipe per published source, nothing else."""
    errors = []
    data = root / "data"
    if not data.is_dir():
        return [{"kind": "converted", "file": str(root), "detail": "no data directory"}]
    expected = {f"gt6ce/recipe/{relative}" for relative in published}
    for relative in sorted(expected):
        target = data / relative
        if not target.is_file():
            errors.append({"kind": "converted", "file": relative, "detail": "missing converted recipe"})
            continue
        try:
            document = json.loads(target.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - report and continue
            errors.append({"kind": "converted", "file": relative, "detail": str(exc)})
            continue
        result = document.get("result")
        if document.get("type") != "gt6ce:tool_repair":
            if not isinstance(result, dict) or not isinstance(result.get("id"), str):
                errors.append({"kind": "converted", "file": relative, "detail": "result has no id"})
        # 1.21.1's ingredient codec rejects a bare string; only value objects and lists are native.
        checked_values = []
        if isinstance(document.get("ingredients"), list):
            checked_values.extend(document["ingredients"])
        if "ingredient" in document:
            checked_values.append(document["ingredient"])
        if isinstance(document.get("key"), dict):
            checked_values.extend(document["key"].values())
        for value in checked_values:
            nested = value if isinstance(value, list) else [value]
            for entry in nested:
                if isinstance(entry, str):
                    errors.append(
                        {"kind": "converted", "file": relative, "detail": "bare string ingredient"}
                    )
    actual = {path.relative_to(data).as_posix() for path in (data / "gt6ce" / "recipe").rglob("*.json")}
    for extra in sorted(actual - expected):
        errors.append({"kind": "converted", "file": extra, "detail": "no matching source"})
    check_converted_tags(data, errors)
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, help="write the full JSON report to this path")
    parser.add_argument(
        "--converted", type=Path, help="cross-check a generated 1.21.1 resource tree"
    )
    parser.add_argument("--strict", action="store_true", help="exit 1 when errors were found")
    args = parser.parse_args()

    recipes, errors = load_recipes()
    warnings = []
    tags = tag_ids()
    items = model_ids()

    tag_refs = []
    signatures = defaultdict(list)
    types = Counter()
    for path, recipe in recipes.items():
        where = rel(path)
        for field in recipe:
            if field not in KNOWN_FIELDS and field not in DOC_FIELDS:
                warnings.append({"kind": "field", "file": where, "detail": field})
        recipe_type = recipe.get("type")
        types[recipe_type] += 1
        if recipe_type not in KNOWN_TYPES:
            errors.append({"kind": "type", "file": where, "detail": repr(recipe_type)})
            continue
        if recipe_type in SHAPED_TYPES:
            check_shaped(recipe, where, errors, tag_refs)
            signatures[recipe_signature(recipe)].append(
                (where, recipe.get("result", {}).get("item"), recipe.get("construction_color", ""))
            )
        elif recipe_type in SHAPELESS_TYPES:
            check_shapeless(recipe, where, errors, tag_refs)
        else:
            if "ingredients" in recipe:
                check_shapeless(recipe, where, errors, tag_refs)
            elif "ingredient" in recipe:
                tag_refs.extend(ingredients(recipe["ingredient"], where, errors))
        check_result(recipe, where, errors)

        for entry in recipe.get("key", {}).values() if isinstance(recipe.get("key"), dict) else []:
            if isinstance(entry, dict) and isinstance(entry.get("item"), str) and CIRCUIT_FAMILY.match(entry["item"]):
                errors.append(
                    {
                        "kind": "circuit-family",
                        "file": where,
                        "detail": f"{entry['item']} should be a cumulative tier tag",
                    }
                )
        for entry in recipe.get("ingredients", []) if isinstance(recipe.get("ingredients"), list) else []:
            if isinstance(entry, dict) and isinstance(entry.get("item"), str) and CIRCUIT_FAMILY.match(entry["item"]):
                errors.append(
                    {
                        "kind": "circuit-family",
                        "file": where,
                        "detail": f"{entry['item']} should be a cumulative tier tag",
                    }
                )

    runtime = runtime_tag_families()
    for ref in tag_refs:
        if ref.startswith("#"):
            ref = ref[1:]
        if not ref.startswith("gt6ce:") or ref in tags:
            continue
        family = ref.split(":", 1)[1].split("/", 1)[0]
        if family in runtime or re.match(r"^(wire|cable)_\d+$", family):
            continue  # generated by MaterialTagPack at runtime
        errors.append({"kind": "tag", "file": "-", "detail": f"missing tag {ref}"})

    for signature, entries in signatures.items():
        if len(entries) < 2:
            continue
        results = {entry[1] for entry in entries}
        if len(results) > 1:
            colors = [entry[2] for entry in entries]
            if any(colors) and len(set(colors)) == len(colors):
                continue  # colour-disambiguated construction variants
            errors.append(
                {
                    "kind": "ambiguous",
                    "file": entries[0][0],
                    "detail": f"{len(entries)} recipes share type+pattern+key: {sorted(results)[:4]}",
                }
            )
        else:
            warnings.append(
                {
                    "kind": "duplicate",
                    "file": entries[0][0],
                    "detail": f"{len(entries)} identical recipes for {entries[0][1]}",
                }
            )

    shipped = {
        path.relative_to(REPO / RECIPE_DIR).as_posix() for path in (REPO / RECIPE_DIR).rglob("*.json")
    }
    # Every shared row is published by the build except the ones named here; those are replaced by
    # a runtime row, so an entry that no longer exists or a row neither published nor excluded is
    # a real defect instead of a warning.
    excluded = load_exclusions()
    excluded_relative = set()
    for entry in sorted(excluded):
        prefix = "gt6ce/recipes/"
        if not entry.startswith(prefix):
            errors.append({"kind": "exclusion", "file": rel(EXCLUSIONS), "detail": f"not a recipe path: {entry}"})
            continue
        relative = entry[len(prefix):]
        if relative not in shipped:
            errors.append({"kind": "exclusion", "file": rel(EXCLUSIONS), "detail": f"stale entry: {entry}"})
        excluded_relative.add(relative)
    published = shipped - excluded_relative

    if args.converted:
        errors.extend(check_converted(Path(args.converted), published))

    material_prefixes = material_prefix_names()
    for path, recipe in recipes.items():
        where = rel(path)
        result = recipe.get("result")
        ids = []
        if isinstance(result, str):
            ids.append(result)
        elif isinstance(result, dict) and isinstance(result.get("item"), str):
            ids.append(result["item"])
        elif isinstance(result, list):
            ids.extend(r.get("item") for r in result if isinstance(r, dict) and isinstance(r.get("item"), str))
        for item in ids:
            if item.startswith("gt6ce:") and any(
                item.startswith("gt6ce:" + prefix + "_") for prefix in material_prefixes
            ):
                continue  # material items get their model from SharedInventoryModelPack
            if item.startswith("gt6ce:") and item not in items:
                warnings.append(
                    {"kind": "item-model", "file": where, "detail": f"no model for {item}"}
                )

    summary = {
        "recipes": len(recipes),
        "published": len(published),
        "excluded": len(excluded_relative),
        "tags": len(shared_tags()),
        "types": dict(sorted(types.items())),
        "errors": len(errors),
        "warnings": len(warnings),
        "errors_by_kind": dict(Counter(e["kind"] for e in errors)),
        "warnings_by_kind": dict(Counter(w["kind"] for w in warnings)),
    }
    print(json.dumps(summary, indent=2, sort_keys=True))
    for label, rows in (("ERROR", errors), ("WARN", warnings)):
        for row in rows[:40]:
            print(f"{label} [{row['kind']}] {row['file']}: {row['detail']}")
        if len(rows) > 40:
            print(f"{label} ... {len(rows) - 40} more")

    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(
                {"summary": summary, "errors": errors, "warnings": warnings},
                indent=2,
                ensure_ascii=False,
                sort_keys=True,
            ),
            encoding="utf-8",
        )
        print(f"report: {args.report}")

    return 1 if args.strict and errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
