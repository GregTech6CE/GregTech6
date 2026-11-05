"""Audit the shipped GT6CE datapack recipe tree (stdlib only).

Usage:
    python tools/recipes/audit_recipes.py [--report PATH] [--strict]

Checks:
  * JSON parse failures and unknown recipe types
  * shaped pattern/key consistency and result shape
  * shapeless and smelting field shape
  * gt6ce: tag references resolve to a tag file (including nested values)
  * concrete circuit-family ingredients where a cumulative tier tag is expected
  * ambiguous shaped recipes (same type+pattern+key, different results)
  * unknown top-level fields (typo guard)
  * hand-maintained catalogs versus the shipped files
  * gt6ce: item ids that have no item model or blockstate (warning only)

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
DATA_ROOTS = [
    Path("core/src/main/resources/data"),
    Path("minecraft/versions/1.20.1-forge/src/main/resources/data"),
    Path("minecraft/versions/1.21.1-neoforge/src/main/resources/data"),
]
ASSET_ROOTS = [
    Path("core/src/main/resources"),
    Path("minecraft/src/main/resources"),
]

KNOWN_TYPES = {
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "minecraft:smelting",
    "gt6ce:tool_shaped",
    "gt6ce:tool_shapeless",
    "gt6ce:book_binding",
    "gt6ce:spike_conversion",
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
SHAPELESS_TYPES = {"minecraft:crafting_shapeless", "gt6ce:tool_shapeless"}

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
    return ids


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


def java_string_list(path: Path, marker: str):
    """Read a `List.of(...)` or `String[] = {...}` literal that follows `marker`."""
    text = path.read_text(encoding="utf-8")
    start = text.find(marker)
    if start < 0:
        return []
    ends = [pos for pos in (text.find(");", start), text.find("};", start)) if pos >= 0]
    if not ends:
        return []
    return re.findall(r'"([^"]+)"', text[start : min(ends)])


# Catalogs re-emitted by the NeoForge recipe packs (plural `recipes/` is not read on 1.21.1).
NEOFORGE_CATALOGS = [
    ("core/src/main/java/gregtech/content/recipe/EquipmentCraftingCatalog.java", ""),
    ("core/src/main/java/gregtech/content/recipe/ClayMoldCatalog.java", ""),
    ("core/src/main/java/gregtech/content/tool/PortableCraftingCatalog.java", "fluid_tools/"),
    ("core/src/main/java/gregtech/content/recipe/AmmunitionCraftingCatalog.java", "ammunition/"),
    ("core/src/main/java/gregtech/content/book/BookCraftingCatalog.java", "books/"),
    ("core/src/main/java/gregtech/content/bumble/BumbleCraftingCatalog.java", "tool_blocks/"),
    ("core/src/main/java/gregtech/content/nuclear/NuclearCraftingCatalog.java", "nuclear/"),
]

# Files another NeoForge pack loads by name.
NEOFORGE_EXPLICIT_FILES = {"tools/repair.json"}

# Static rows replaced by runtime generators (FormConversionCraftingRecipeInjector and friends).
NEOFORGE_GENERATED_PREFIXES = (
    "wire_working/",
    "axial/",
    "polymer_forming/",
    "hand/pipe/",
    "hand_material/rocks/",
    "machines/basic/massfab_",
    "machines/multiblock/",
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, help="write the full JSON report to this path")
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
    listed = set()
    for relative, prefix in NEOFORGE_CATALOGS:
        path = REPO / relative
        entries = {prefix + entry for entry in java_string_list(path, "FILES")}
        listed |= entries
        for missing in sorted(entries - shipped):
            errors.append({"kind": "catalog", "file": rel(path), "detail": f"listed but missing: {missing}"})
    # On 1.21.1 only `recipe/` is read, so a plural `recipes/` file must be re-emitted by a
    # pack or replaced by a runtime generator; otherwise it is silently dead.
    for file in sorted(shipped - listed - NEOFORGE_EXPLICIT_FILES):
        if not file.startswith(NEOFORGE_GENERATED_PREFIXES):
            warnings.append(
                {"kind": "neoforge-unlisted", "file": file, "detail": "not re-emitted on 1.21.1"}
            )

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
