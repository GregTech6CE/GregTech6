#!/usr/bin/env python3
"""Generate MT.java material field aliases and MaterialModTags.java from GT6Materials + GT6 MT.init()."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import re
from pathlib import Path
from modular_materials import parse_catalog, write_readable_references
from readable_data_names import readable_java

ROOT = Path(__file__).resolve().parents[1]
GT6_MT = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MT.java")
GT6_MATERIALS = ROOT / "tools/generated/materials-source.java.txt"
MT_JAVA = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/ImportedMaterialData.java")
MOD_TAGS_OUT = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/generated/MaterialModTags.java")

BEGIN = "    // <generated-mt-fields>"
END = "    // </generated-mt-fields>"

FIELD_RE = re.compile(r"^\s+(\w+)\s*=\s*(?:\w+\.)?(?:create|metal|element|alloy|ore|gem|dust|gas|wood|stone|elec|cent|clay)\(")
CLASS_RE = re.compile(r"public static final class (\w+)")
ALIAS_RE = re.compile(r"^\s+(\w+)\s*=\s*(\w+)\s*,?\s*$")
MOD_TAG_RE = re.compile(r"^\s*(?:(STONES|WOODS|OREMATS)\.)?(\w+)\s+\.put\(MD\.(\w+)")
NESTED_CLASS = ("OREMATS", "STONES", "WOODS")
SKIP_TOP = frozenset({"NULL", "Empty", "Ma", "Magic"})


def load_material_paths(java_path: Path) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    by_bucket: dict[str, dict[str, str]] = {}
    if java_path.is_file():
        bucket: str | None = None
        for line in java_path.read_text(encoding="utf-8").splitlines():
            class_match = CLASS_RE.search(line)
            if class_match:
                bucket = class_match.group(1)
                continue
            field_match = FIELD_RE.match(line)
            if bucket and field_match:
                field = field_match.group(1)
                by_bucket.setdefault(bucket, {})[field] = f"GT6Materials.{bucket}.{field}"

    flat: dict[str, str] = {"Ma": "MT.Ma", "Magic": "MT.Ma", "NULL": "MT.NULL", "Empty": "MT.Empty"}
    for bucket in ("Elements", "Compounds", "Ores", "Stones", "Woods"):
        flat.update(by_bucket.get(bucket, {}))
    return flat, by_bucket


def parse_gt6_nested_fields(text: str, class_name: str) -> list[str]:
    marker = f"public static class {class_name} {{"
    start = text.find(marker)
    if start < 0:
        return []
    depth = 0
    body: list[str] = []
    for ch in text[start:]:
        if ch == "{":
            depth += 1
            if depth == 1:
                continue
        elif ch == "}":
            depth -= 1
            if depth == 0:
                break
        if depth >= 1:
            body.append(ch)
    fields: list[str] = []
    for line in "".join(body).splitlines():
        match = re.match(r"^\t\t(\w+)\s*=", line)
        if match:
            fields.append(match.group(1))
    return fields


def parse_gt6_stone_aliases(text: str) -> list[tuple[str, str]]:
    block = re.search(
        r"/\*\* Moved most Stones to their own Class\.\s*\*/\s*"
        r"@Deprecated public static final OreDictMaterial\s+(.*?);",
        text,
        re.S,
    )
    if not block:
        return []
    aliases: list[tuple[str, str]] = []
    for line in block.group(1).splitlines():
        match = re.match(r"^\s*(\w+)\s*=\s*STONES\.(\w+)\s*,?\s*$", line.strip())
        if match:
            aliases.append((match.group(1), match.group(2)))
    return aliases


def load_gt6_aliases(text: str, known_fields: set[str]) -> list[tuple[str, str]]:
    aliases: list[tuple[str, str]] = []
    for line in text.splitlines():
        match = ALIAS_RE.match(line.strip())
        if not match:
            continue
        alias, target = match.group(1), match.group(2)
        if alias == target:
            continue
        if alias in known_fields and target in known_fields:
            aliases.append((alias, target))
    return aliases


def parse_mod_tags(text: str) -> list[tuple[str, str, str | None]]:
    tags: list[tuple[str, str, str | None]] = []
    block = re.search(r"public static class TECH.*?static void init\(\)\s*\{(.*?)\n\t\t\}", text, re.S)
    if not block:
        return tags
    for line in block.group(1).splitlines():
        match = MOD_TAG_RE.match(line)
        if match:
            prefix, field, md = match.group(1), match.group(2), match.group(3)
            tags.append((field, md, prefix))
    return tags


def resolve_mod_ref(
    field: str,
    prefix: str | None,
    paths: dict[str, str],
    by_bucket: dict[str, dict[str, str]],
    oremats_fields: set[str],
) -> str | None:
    if prefix == "STONES":
        return f"MT.STONES.{field}" if field in by_bucket.get("Stones", {}) else None
    if prefix == "WOODS":
        return f"MT.WOODS.{field}" if field in by_bucket.get("Woods", {}) else None
    if prefix == "OREMATS":
        return f"MT.OREMATS.{field}" if field in oremats_fields and oremats_ref(field, by_bucket) else None
    if field in by_bucket.get("Woods", {}):
        return f"MT.WOODS.{field}"
    if field in by_bucket.get("Stones", {}):
        return f"MT.STONES.{field}"
    if field in oremats_fields and oremats_ref(field, by_bucket):
        return f"MT.OREMATS.{field}"
    if field not in paths:
        return None
    ref = paths[field]
    if ref.startswith("MT."):
        return ref
    return f"MT.{field}"


def join_entries(entries: list[str], indent: str) -> str:
    if not entries:
        return f"{indent}NULL;"
    return ",\n".join(entries) + ";"


def oremats_ref(name: str, by_bucket: dict[str, dict[str, str]]) -> str | None:
    for bucket in ("Ores", "Compounds"):
        ref = by_bucket.get(bucket, {}).get(name)
        if ref:
            return ref
    return None


def bucket_ref(name: str, bucket: str, by_bucket: dict[str, dict[str, str]]) -> str | None:
    return by_bucket.get(bucket, {}).get(name)


def merge_field_order(gt6_order: list[str], bucket_fields: set[str]) -> list[str]:
    ordered = [name for name in gt6_order if name in bucket_fields]
    extras = sorted(bucket_fields - set(ordered))
    return ordered + extras


def emit_nested_class(
    class_name: str,
    comment: str,
    field_names: list[str],
    *,
    ref_builder,
) -> list[str]:
    entries = [f"            {name} = {ref}" for name in field_names if (ref := ref_builder(name))]
    lines = [f"    /** {comment} */", f"    public static final class {class_name} {{", f"        private {class_name}() {{}}", ""]
    if entries:
        lines.extend(["        public static final GTMaterial", join_entries(entries, "            ")])
    lines.append("    }")
    return lines


def emit_mt_fields(
    paths: dict[str, str],
    by_bucket: dict[str, dict[str, str]],
    aliases: list[tuple[str, str]],
    gt6_text: str,
) -> str:
    oremats_order = parse_gt6_nested_fields(gt6_text, "OREMATS")
    stones_order = merge_field_order(parse_gt6_nested_fields(gt6_text, "STONES"), set(by_bucket.get("Stones", {})))
    woods_order = merge_field_order(parse_gt6_nested_fields(gt6_text, "WOODS"), set(by_bucket.get("Woods", {})))
    stone_deprecated = parse_gt6_stone_aliases(gt6_text)

    nested_fields = set(oremats_order) | set(stones_order) | set(woods_order)
    nested_fields |= set(by_bucket.get("Stones", {}))
    nested_fields |= set(by_bucket.get("Woods", {}))

    top_entries: list[str] = []
    for field in sorted(paths.keys()):
        if field in SKIP_TOP or field in nested_fields:
            continue
        ref = paths[field]
        if ref.startswith("MT."):
            continue
        top_entries.append(f"            {field} = {ref}")

    for alias, target in sorted(aliases, key=lambda x: x[0]):
        if alias in nested_fields or target in nested_fields:
            continue
        if any(alias in entry for entry in top_entries):
            continue
        if target not in paths:
            continue
        top_entries.append(f"            {alias} = {target}")

    lines = [
        BEGIN,
        "    /** Elements, compounds, and general materials (GT6 top-level {@code MT.*}). Auto-generated — do not edit. */",
        "    public static final GTMaterial",
        join_entries(top_entries, "            "),
        "",
    ]

    lines.extend(
        emit_nested_class(
            "OREMATS",
            "Advanced ore-processing minerals (GT6 {@code MT.OREMATS}).",
            oremats_order,
            ref_builder=lambda name: oremats_ref(name, by_bucket),
        )
    )
    lines.append("")
    lines.extend(
        emit_nested_class(
            "STONES",
            "Stone variants moved out of the main list for readability (GT6 {@code MT.STONES}).",
            stones_order,
            ref_builder=lambda name: bucket_ref(name, "Stones", by_bucket),
        )
    )
    lines.append("")
    lines.extend(
        emit_nested_class(
            "WOODS",
            "Wood variants (GT6 {@code MT.WOODS}).",
            woods_order,
            ref_builder=lambda name: bucket_ref(name, "Woods", by_bucket),
        )
    )

    dep_entries = [
        f"            {alias} = STONES.{target}"
        for alias, target in stone_deprecated
        if target in paths and target in stones_order
    ]
    if dep_entries:
        lines.extend(
            [
                "",
                "    /** GT6 compatibility — stone materials kept at top level. */",
                "    @Deprecated",
                "    public static final GTMaterial",
                join_entries(dep_entries, "            "),
            ]
        )

    lines.append(END)
    return "\n".join(lines)


def emit_mod_tags(
    tags: list[tuple[str, str, str | None]],
    paths: dict[str, str],
    by_bucket: dict[str, dict[str, str]],
    md_fields: set[str],
    oremats_fields: set[str],
) -> str:
    seen: set[str] = set()
    apply_lines: list[str] = []
    for field, md, prefix in tags:
        if md not in md_fields:
            continue
        ref = resolve_mod_ref(field, prefix, paths, by_bucket, oremats_fields)
        if not ref or ref in seen:
            continue
        seen.add(ref)
        apply_lines.append(f"        {ref}.mod(MD.{md});")

    # Minecraft 1.20+ native materials (GT6 may tag Cu as Et Futurum backport).
    mc_fields = ("MT.Au", "MT.Cu", "MT.Fe")
    apply_lines = [line for line in apply_lines if not any(line.startswith(f"        {f}.mod(") for f in mc_fields)]
    apply_lines.extend(f"        {f}.mod(MD.MC);" for f in mc_fields)

    return (
        "package com.gregtech.gregtech.data.generated;\n\n"
        "import com.gregtech.gregtech.data.MD;\n"
        "import com.gregtech.gregtech.data.ImportedMaterialData;\n\n"
        "/** Applies GT6 TECH.init() mod provenance tags. Auto-generated. */\n"
        "public final class MaterialModTags {\n"
        "    private MaterialModTags() {}\n\n"
        "    public static void apply() {\n"
        + "\n".join(sorted(set(apply_lines)))
        + "\n    }\n"
        "}\n"
    )


def patch_mt_java(block: str) -> None:
    text = MT_JAVA.read_text(encoding="utf-8")
    if BEGIN not in text:
        marker = "    // <generated-mt-fields>"
        text = text.replace("}\n", block + "\n}\n", 1) if marker not in text else text
    else:
        start = text.index(BEGIN)
        end = text.index(END) + len(END)
        text = text[:start] + block + text[end:]
    if "import com.gregtech.gregtech.data.generated.GT6Materials;" not in text:
        text = text.replace(
            "import com.gregtech.gregtech.api.material.MaterialTextureSet;",
            "import com.gregtech.gregtech.api.material.MaterialTextureSet;\n"
            "import com.gregtech.gregtech.data.generated.GT6Materials;",
        )
    MT_JAVA.write_text(text, encoding="utf-8")


def load_md_fields(md_path: Path) -> set[str]:
    fields: set[str] = set()
    if not md_path.is_file():
        return fields
    for match in re.finditer(r"(\w+)\s*=\s*new ModData\(", md_path.read_text(encoding="utf-8")):
        fields.add(match.group(1))
    return fields


def main() -> None:
    paths, by_bucket = load_material_paths(GT6_MATERIALS)
    gt6_text = GT6_MT.read_text(encoding="utf-8", errors="ignore")
    aliases = load_gt6_aliases(gt6_text, set(paths.keys()))
    block = emit_mt_fields(paths, by_bucket, aliases, gt6_text)
    patch_mt_java(block)
    tags = parse_mod_tags(gt6_text)
    md_fields = load_md_fields(native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/ModReferences.java"))
    MOD_TAGS_OUT.parent.mkdir(parents=True, exist_ok=True)
    MOD_TAGS_OUT.write_text(
        readable_java(emit_mod_tags(tags, paths, by_bucket, md_fields, set(parse_gt6_nested_fields(gt6_text, "OREMATS")))),
        encoding="utf-8",
    )
    write_readable_references(parse_catalog(GT6_MATERIALS.read_text(encoding="utf-8")), MT_JAVA.parent.parent)
    nested = sum(len(parse_gt6_nested_fields(gt6_text, cls)) for cls in NESTED_CLASS)
    top = block.count("\n            ") - nested * 2
    print(f"Patched MT.java: ~{top} top-level fields, nested OREMATS/STONES/WOODS")
    print(f"Wrote MaterialModTags.java: {len(tags)} mod-tag bindings parsed")


if __name__ == "__main__":
    main()
