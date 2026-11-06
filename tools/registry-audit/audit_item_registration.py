"""Audit how GT6CE registers items and blocks (stdlib only).

Usage:
    python tools/registry-audit/audit_item_registration.py [--root PATH] [--out DIR] [--json] [--strict]

Checks:
  * DeferredRegister declarations by registry kind and source set
  * literal ids passed to .register("...") and duplicates within one registry kind
  * registration entry-point conventions in content/registry and *Registration classes
  * hardcoded "gt6ce" namespace literals in registration packages
  * definition tables (Catalog/Definitions/Rules/Gen) referenced by registry code

Stonecutter keeps the inactive branch inside block comments. Both branches are
scanned on purpose so platform divergence stays visible; dead commented code can
therefore appear in the unclassified list and is for manual review.

Exit code is 0 unless --strict is given and duplicate literal ids or hardcoded
namespace literals in registration packages were found.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

SOURCE_SETS = {
    "core": Path("core/src/main/java"),
    "shared": Path("minecraft/src/main/java"),
    "1.20.1-forge": Path("minecraft/versions/1.20.1-forge/src/main/java"),
    "1.21.1-neoforge": Path("minecraft/versions/1.21.1-neoforge/src/main/java"),
}

KIND_BY_TYPE = {
    "Item": "ITEM",
    "Items": "ITEM",
    "Block": "BLOCK",
    "Blocks": "BLOCK",
    "BlockEntityType": "BLOCK_ENTITY_TYPE",
    "MenuType": "MENU",
    "RecipeSerializer": "RECIPE_SERIALIZER",
    "RecipeType": "RECIPE_TYPE",
    "SoundEvent": "SOUND_EVENT",
    "EntityType": "ENTITY_TYPE",
    "Fluid": "FLUID",
    "CreativeModeTab": "CREATIVE_TAB",
    "Enchantment": "ENCHANTMENT",
    "ParticleType": "PARTICLE",
    "MobEffect": "MOB_EFFECT",
    "Attribute": "ATTRIBUTE",
    "DataComponentType": "DATA_COMPONENT",
    "Feature": "FEATURE",
    "StructureType": "STRUCTURE_TYPE",
    "MapCodec": "MAP_CODEC",
    "ArmorMaterial": "ARMOR_MATERIAL",
    "IngredientType": "INGREDIENT_TYPE",
    "StructurePieceType": "STRUCTURE_PIECE_TYPE",
    "StructurePlacementType": "STRUCTURE_PLACEMENT_TYPE",
}

DR_GENERIC = re.compile(
    r"DeferredRegister\s*<\s*([\w.]+)(?:\s*<[^;{}]*?>)?\s*>\s+([A-Za-z_]\w*)\s*[=;]"
)
DR_DOT = re.compile(r"DeferredRegister\.(\w+)\s+([A-Za-z_]\w*)\s*=")
CALL = re.compile(
    r"([A-Za-z_][\w.]*(?:\([^;{}]*?\))?)\s*\.\s*register\s*\(\s*\"([a-z0-9_./-]+)\""
)
NAMESPACE_LITERAL = re.compile(r"\"gt6ce\"")
NAMESPACE_LITERAL_ARG = re.compile(r"DeferredRegister\.create\w*\s*\([^;]*?\"([a-z0-9_]+)\"")
NAMESPACE_CONSTANT = re.compile(
    r"DeferredRegister\.create\w*\s*\([^;]*?\b(?:GregTech|GregTechNeoForge)\.(?:NAMESPACE|MOD_ID)\b"
    r"|DeferredRegister\.create\w*\s*\([^;]*?\bGregTechIdentity\.(?:REGISTRY_NAMESPACE|MOD_ID)\b"
    r"|DeferredRegister\.create\w*\s*\([^;]*?\bREGISTRY_NAMESPACE\b"
)
STONECUTTER_IF = re.compile(r"//\?\s*if\s+([^\{]+?)\s*\{")
STONECUTTER_ELSE = re.compile(r"//\?\s*\}\s*else\s*\{")
STONECUTTER_END = re.compile(r"//\?\s*\}")
STONECUTTER_BRANCHES = {
    ">=1.21.1": ("1.21.1-neoforge", "1.20.1-forge"),
    "<1.21.1": ("1.20.1-forge", "1.21.1-neoforge"),
}
PLATFORMS = ("1.20.1-forge", "1.21.1-neoforge")
CENTRAL_NAMESPACE = {
    "GTItems.ITEMS": "gt6ce",
    "GTBlocks.BLOCK_ITEMS": "gt6ce",
    "GTBlocks.BLOCKS": "gt6ce",
    "GTTechnological.ITEMS": "gt6ce",
    "GTMultiItems.ITEMS": "gt6ce",
}
DEFINITION_SOURCE = re.compile(
    r"\b([A-Z][A-Za-z0-9_]*(?:Catalog|Definitions|Rules|Gen))\b"
)
ENTRY_METHODS = {
    "registerAll": re.compile(r"\bstatic\b[^;{}()]*\bregisterAll\s*\("),
    "initialize": re.compile(r"\bstatic\b[^;{}()]*\binitialize\s*\("),
    "bootstrap": re.compile(r"\bstatic\b[^;{}()]*\bbootstrap\s*\("),
    "submit": re.compile(r"\bstatic\b[^;{}()]*\bsubmit\s*\("),
    "register(bus)": re.compile(
        r"\bstatic\b[^;{}()]*\bregister\s*\([^)]*IEventBus\b"
    ),
}

REGISTRATION_DIR_PARTS = (
    "/content/registry/",
    "/bootstrap/",
    "/platform/neoforge/registry/",
    "/platform/neoforge/machine/",
    "/platform/neoforge/fluid/",
    "/platform/neoforge/logistics/",
    "/platform/neoforge/smeltery/",
    "/platform/neoforge/transport/",
    "/content/transport/",
)
REGISTRATION_FILE_NAMES = ("Registration.java", "Registries.java", "MachineRegistry.java")


def is_registration_file(rel: str) -> bool:
    path = "/" + rel.replace("\\", "/")
    return any(part in path for part in REGISTRATION_DIR_PARTS) or rel.endswith(
        REGISTRATION_FILE_NAMES
    )


def source_set_of(rel: str) -> str:
    normalized = rel.replace("\\", "/")
    for name, prefix in SOURCE_SETS.items():
        if normalized.startswith(prefix.as_posix() + "/"):
            return name
    return "other"


def java_files(root: Path):
    for name, prefix in SOURCE_SETS.items():
        base = root / prefix
        if not base.is_dir():
            continue
        for path in sorted(base.rglob("*.java")):
            rel = path.relative_to(root).as_posix()
            yield name, rel, path.read_text(encoding="utf-8", errors="ignore")


def normalize_kind(raw: str) -> str:
    raw = raw.strip()
    simple = raw.split("<", 1)[0].rsplit(".", 1)[-1]
    if simple in KIND_BY_TYPE:
        return KIND_BY_TYPE[simple]
    for key, value in KIND_BY_TYPE.items():
        if simple.startswith(key + "<"):
            return value
    return simple.upper()


def statement_namespace(statement: str) -> str | None:
    match = NAMESPACE_LITERAL_ARG.search(statement)
    if match:
        return match.group(1)
    if NAMESPACE_CONSTANT.search(statement):
        return "gt6ce"
    return None


def file_line_platforms(text: str, source_set: str) -> list[set[str]]:
    if source_set in PLATFORMS:
        return [{source_set}] * len(text.splitlines())
    return line_platforms(text)


def line_platforms(text: str) -> list[set[str]]:
    """Platform set per line; shared lines apply to both, Stonecutter branches to one."""
    active: list[set[str]] = []
    conditions: list[str] = []
    result: list[set[str]] = []
    for line in text.splitlines():
        current = set(PLATFORMS)
        for platforms in active:
            current &= platforms
        result.append(current)

        if_match = STONECUTTER_IF.search(line)
        if if_match:
            condition = if_match.group(1).strip()
            branches = STONECUTTER_BRANCHES.get(condition, PLATFORMS)
            active.append({branches[0]})
            conditions.append(condition)
            continue
        if STONECUTTER_ELSE.search(line):
            if active:
                condition = conditions[-1] if conditions else ""
                branches = STONECUTTER_BRANCHES.get(condition, PLATFORMS)
                active[-1] = {branches[1]}
            continue
        if STONECUTTER_END.search(line):
            if active:
                active.pop()
            if conditions:
                conditions.pop()
    return result


def declarations(text: str, platforms_by_line: list[set[str]]):
    lines = text.splitlines()
    for index, line in enumerate(lines):
        # A field declaration may put DeferredRegister.create(...) on the next line.
        statement = " ".join(lines[index : index + 6])
        namespace = statement_namespace(statement)
        platforms = platforms_by_line[index] if index < len(platforms_by_line) else set(PLATFORMS)
        for match in DR_GENERIC.finditer(line):
            yield index + 1, match.group(2), normalize_kind(match.group(1)), namespace, platforms
        for match in DR_DOT.finditer(line):
            yield index + 1, match.group(2), normalize_kind(match.group(1)), namespace, platforms


def receiver_namespace(receiver: str, fallback: str | None) -> str | None:
    match = re.search(r"\"([a-z0-9_]+)\"", receiver)
    if match:
        return match.group(1)
    if "NAMESPACE" in receiver:
        return "gt6ce"
    return fallback


def pick_candidate(candidates, platforms):
    if not candidates:
        return None
    matching = [c for c in candidates if c[2] & platforms]
    pool = matching or candidates
    return pool[-1][:2]


def resolve_receiver(receiver: str, platforms, local, global_kind):
    based_call = receiver.split("(", 1)[0].strip()
    if based_call in ("items", "blockItems") or based_call.endswith((".items", ".blockItems")):
        return "ITEM", receiver_namespace(receiver, None)
    if based_call == "blocks" or based_call.endswith(".blocks"):
        return "BLOCK", receiver_namespace(receiver, None)
    if based_call in ("RegisteredBlock", "gregtech.content.registry.RegisteredBlock"):
        return "BLOCK", "gt6ce"
    if based_call in ("RegisteredItem", "gregtech.content.registry.RegisteredItem"):
        return "ITEM", "gt6ce"
    base = based_call
    if base in CENTRAL_NAMESPACE:
        kind = "ITEM" if base.endswith(("ITEMS", "BLOCK_ITEMS")) else "BLOCK"
        return kind, CENTRAL_NAMESPACE[base]
    if "." in base:
        qualifier, _, variable = base.rpartition(".")
        qualifier = qualifier.rsplit(".", 1)[-1]
        full = f"{qualifier}.{variable}"
        if full in CENTRAL_NAMESPACE:
            kind = "ITEM" if full.endswith(("ITEMS", "BLOCK_ITEMS")) else "BLOCK"
            return kind, CENTRAL_NAMESPACE[full]
        return pick_candidate(global_kind.get((qualifier, variable)), platforms)
    return pick_candidate(local.get(base), platforms)


def markdown_table(rows, headers):
    lines = ["| " + " | ".join(headers) + " |", "| " + " | ".join("---" for _ in headers) + " |"]
    for row in rows:
        lines.append("| " + " | ".join(str(cell) for cell in row) + " |")
    return lines


def audit(root: Path) -> dict:
    files = list(java_files(root))

    global_kind: dict[tuple[str, str], list[tuple[str, str | None, set[str]]]] = defaultdict(list)
    local_kinds: dict[str, dict[str, list[tuple[str, str | None, set[str]]]]] = {}
    declaration_rows = []
    for set_name, rel, text in files:
        class_name = Path(rel).stem
        platforms_by_line = file_line_platforms(text, set_name)
        local: dict[str, list[tuple[str, str | None, set[str]]]] = defaultdict(list)
        for line_number, variable, kind, namespace, platforms in declarations(
            text, platforms_by_line
        ):
            local[variable].append((kind, namespace, platforms))
            global_kind[(class_name, variable)].append((kind, namespace, platforms))
            declaration_rows.append(
                {
                    "source_set": set_name,
                    "file": rel,
                    "line": line_number,
                    "variable": variable,
                    "kind": kind,
                    "namespace": namespace,
                    "platforms": sorted(platforms),
                }
            )
        local_kinds[rel] = local

    literal_by_kind = defaultdict(Counter)
    literal_locations = defaultdict(list)
    unclassified = []
    namespace_rows = []
    entry_rows = []
    definition_sources = defaultdict(set)

    for set_name, rel, text in files:
        local = local_kinds.get(rel, {})
        platforms_by_line = file_line_platforms(text, set_name)

        for match in CALL.finditer(text):
            receiver, item_id = match.group(1).strip(), match.group(2)
            line_index = text.count("\n", 0, match.start())
            call_platforms = platforms_by_line[line_index] if line_index < len(platforms_by_line) else set(PLATFORMS)
            resolved = resolve_receiver(receiver, call_platforms, local, global_kind)
            line_number = line_index + 1
            if resolved:
                kind, namespace = resolved
                literal_by_kind[kind][item_id] += 1
                literal_locations[kind].append(
                    {
                        "id": item_id,
                        "file": rel,
                        "line": line_number,
                        "receiver": receiver,
                        "namespace": namespace,
                        "platforms": sorted(call_platforms),
                    }
                )
            else:
                unclassified.append(
                    {"file": rel, "line": line_number, "receiver": receiver, "id": item_id}
                )

        if is_registration_file(rel):
            hits = [m.start() for m in NAMESPACE_LITERAL.finditer(text)]
            if hits:
                namespace_rows.append(
                    {
                        "source_set": set_name,
                        "file": rel,
                        "count": len(hits),
                        "lines": [text.count("\n", 0, hit) + 1 for hit in hits],
                    }
                )
            methods = sorted(name for name, pattern in ENTRY_METHODS.items() if pattern.search(text))
            if methods:
                entry_rows.append(
                    {"source_set": set_name, "file": rel, "methods": methods}
                )
            for name in sorted(set(DEFINITION_SOURCE.findall(text))):
                definition_sources[name].add(rel)

    duplicates = []
    unknown_namespace_locations = []
    for kind, counter in sorted(literal_by_kind.items()):
        for item_id in sorted(counter):
            locations = [row for row in literal_locations[kind] if row["id"] == item_id]
            known = [row for row in locations if row["namespace"] is not None]
            if not known:
                unknown_namespace_locations.append(
                    {
                        "kind": kind,
                        "id": item_id,
                        "files": sorted({row["file"] for row in locations}),
                    }
                )
                continue
            for platform in PLATFORMS:
                by_namespace = defaultdict(list)
                for row in known:
                    if platform in row["platforms"]:
                        by_namespace[row["namespace"]].append(row)
                for namespace, rows in sorted(by_namespace.items()):
                    sites = sorted({(row["file"], row["line"]) for row in rows})
                    if len(sites) > 1:
                        duplicates.append(
                            {
                                "platform": platform,
                                "kind": kind,
                                "namespace": namespace,
                                "id": item_id,
                                "locations": [
                                    {"file": file, "line": line} for file, line in sites
                                ],
                                "files": sorted({file for file, _ in sites}),
                            }
                        )

    declarations_by_set = Counter(row["source_set"] for row in declaration_rows)
    item_registries = [row for row in declaration_rows if row["kind"] == "ITEM"]
    item_registry_files = sorted({row["file"] for row in item_registries})

    return {
        "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "root": str(root),
        "summary": {
            "java_files_scanned": len(files),
            "registration_files": sum(1 for _, rel, _ in files if is_registration_file(rel)),
            "deferred_registers": len(declaration_rows),
            "item_deferred_registers": len(item_registries),
            "item_deferred_register_files": len(item_registry_files),
            "literal_registrations": sum(
                sum(counter.values()) for counter in literal_by_kind.values()
            ),
            "literal_by_kind": {
                kind: sum(counter.values()) for kind, counter in sorted(literal_by_kind.items())
            },
            "duplicate_literal_ids": len(duplicates),
            "unknown_namespace_literal_ids": len(unknown_namespace_locations),
            "unclassified_literal_calls": len(unclassified),
            "hardcoded_namespace_literals": sum(row["count"] for row in namespace_rows),
        },
        "registries_by_source_set": dict(sorted(declarations_by_set.items())),
        "item_registry_files": item_registry_files,
        "item_registry_declarations": item_registries,
        "entry_points": entry_rows,
        "hardcoded_namespace": namespace_rows,
        "duplicate_literal_ids": duplicates,
        "unknown_namespace_literal_ids": unknown_namespace_locations,
        "unclassified_registrations": unclassified,
        "definition_sources": {
            name: sorted(paths) for name, paths in sorted(definition_sources.items())
        },
    }


def render_markdown(report: dict) -> str:
    summary = report["summary"]
    lines = [
        "# Item registration audit",
        "",
        f"- Generated: `{report['generated_at']}`",
        f"- Root: `{report['root']}`",
        "",
        "## Summary",
        "",
    ]
    lines += markdown_table(
        [
            ("Java files scanned", summary["java_files_scanned"]),
            ("Registration files", summary["registration_files"]),
            ("DeferredRegister declarations", summary["deferred_registers"]),
            (
                "Item DeferredRegister declarations (both Stonecutter branches)",
                summary["item_deferred_registers"],
            ),
            ("Files declaring an item DeferredRegister", summary["item_deferred_register_files"]),
            ("Literal `.register(\"id\")` calls", summary["literal_registrations"]),
            ("Duplicate literal ids in one namespace + kind", summary["duplicate_literal_ids"]),
            ("Literal ids with an unresolved namespace", summary["unknown_namespace_literal_ids"]),
            ("Unclassified literal register calls", summary["unclassified_literal_calls"]),
            ("Hardcoded `\"gt6ce\"` in registration packages", summary["hardcoded_namespace_literals"]),
        ],
        ("metric", "value"),
    )

    lines += ["", "## Registries by source set", ""]
    lines += markdown_table(
        [(name, count) for name, count in report["registries_by_source_set"].items()],
        ("source set", "DeferredRegister declarations"),
    )

    lines += ["", "## Item registry declarations", ""]
    lines += markdown_table(
        [
            (
                row["source_set"],
                f"`{row['file']}:{row['line']}`",
                row["variable"],
                row["kind"],
                row["namespace"] or "(dynamic)",
                ", ".join(row["platforms"]),
            )
            for row in report["item_registry_declarations"]
        ],
        ("source set", "location", "variable", "kind", "namespace", "platforms"),
    )

    lines += ["", "## Entry-point conventions", ""]
    lines += markdown_table(
        [
            (row["source_set"], f"`{row['file']}`", ", ".join(f"`{m}`" for m in row["methods"]))
            for row in report["entry_points"]
        ],
        ("source set", "file", "methods"),
    )

    lines += ["", "## Hardcoded namespace literals", ""]
    lines += markdown_table(
        [(row["source_set"], f"`{row['file']}`", row["count"]) for row in report["hardcoded_namespace"]],
        ("source set", "file", "\"gt6ce\" count"),
    )

    lines += ["", "## Duplicate literal ids in one namespace + kind", ""]
    if report["duplicate_literal_ids"]:
        lines += markdown_table(
            [
                (
                    row["platform"],
                    row["kind"],
                    row["namespace"],
                    row["id"],
                    "<br>".join(f"`{site['file']}:{site['line']}`" for site in row["locations"]),
                )
                for row in report["duplicate_literal_ids"]
            ],
            ("platform", "kind", "namespace", "id", "locations"),
        )
    else:
        lines.append("None.")

    lines += ["", "## Literal ids with an unresolved namespace", ""]
    if report["unknown_namespace_literal_ids"]:
        limit = 40
        for row in report["unknown_namespace_literal_ids"][:limit]:
            lines.append(
                f"- {row['kind']} `{row['id']}` in "
                + ", ".join(f"`{path}`" for path in row["files"])
            )
        if len(report["unknown_namespace_literal_ids"]) > limit:
            lines.append(
                f"- ... {len(report['unknown_namespace_literal_ids']) - limit} more in `audit.json`"
            )
    else:
        lines.append("None.")

    lines += ["", "## Unclassified literal register calls", ""]
    unclassified = report["unclassified_registrations"]
    if unclassified:
        limit = 60
        for row in unclassified[:limit]:
            lines.append(
                f"- `{row['file']}:{row['line']}` receiver `{row['receiver']}` id `{row['id']}`"
            )
        if len(unclassified) > limit:
            lines.append(f"- ... {len(unclassified) - limit} more in `audit.json`")
    else:
        lines.append("None.")

    lines += ["", "## Definition tables referenced by registry code", ""]
    for name, paths in report["definition_sources"].items():
        lines.append(f"- `{name}` ({len(paths)} file(s))")

    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="repository root (default: the checkout containing this script)",
    )
    parser.add_argument("--out", type=Path, help="write audit.json and audit.md here")
    parser.add_argument("--json", action="store_true", help="print JSON instead of a summary")
    parser.add_argument(
        "--strict",
        action="store_true",
        help="exit 1 on duplicate literal ids or hardcoded namespace in registration packages",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    report = audit(root)
    markdown = render_markdown(report)

    if args.out:
        args.out.mkdir(parents=True, exist_ok=True)
        (args.out / "audit.json").write_text(
            json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
        (args.out / "audit.md").write_text(markdown, encoding="utf-8")

    if args.json:
        print(json.dumps(report, indent=2, ensure_ascii=False))
    else:
        print(markdown)

    if args.strict:
        has_problems = (
            report["summary"]["duplicate_literal_ids"] > 0
            or report["summary"]["hardcoded_namespace_literals"] > 0
        )
        return 1 if has_problems else 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
