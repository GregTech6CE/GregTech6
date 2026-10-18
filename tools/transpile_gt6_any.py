#!/usr/bin/env python3
"""Transpile GregTech 6 ANY.java into GregTech6 ANY.java."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
from pathlib import Path
from readable_data_names import readable_java

GT6_ANY = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\ANY.java")
GT6_MATERIALS = (
    Path(__file__).resolve().parents[1]
    / "tools/generated/materials-source.java.txt"
)
OUT = native_path(Path(__file__).resolve().parents[1] / "src/main/java/com/gregtech/gregtech/data/MaterialGroups.java")

ANY_DECL_RE = re.compile(r"(\w+)\s*=\s*any\(\"((?:\\.|[^\"\\])*)\"\s*\)")
LOCAL_RE = re.compile(r'\.setLocal\("((?:\\.|[^"\\])*)"\)')
REREG_RE = re.compile(r"\.addReRegistrationToThis\(([^)]*)\)")
SKIP_INIT_FIELDS = frozenset({"INITIALIZED", "if", "return"})


def java_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def load_material_paths(java_path: Path) -> dict[str, str]:
    mapping: dict[str, str] = {"Ma": "MT.Ma", "Magic": "MT.Ma"}
    if not java_path.is_file():
        return mapping
    bucket: str | None = None
    for line in java_path.read_text(encoding="utf-8").splitlines():
        class_match = re.search(r"public static final class (\w+)", line)
        if class_match:
            bucket = class_match.group(1)
            continue
        field_match = re.search(
            r"(\w+)\s*=\s*(?:elec|cent|clay|gem|dust|metal|element|alloy|ore|gas|wood|woodNormal|stone)\(",
            line,
        )
        if bucket and field_match:
            field = field_match.group(1)
            mapping[field] = f"GT6Materials.{bucket}.{field}"
    return mapping


def parse_declarations(text: str) -> list[tuple[str, str, str | None]]:
    """field, registry name, optional alias-of field."""
    decls: list[tuple[str, str, str | None]] = []
    blocks = re.findall(
        r"public static final OreDictMaterial\s*(.*?);",
        text,
        re.S,
    )
    primary: set[str] = set()
    for chunk in blocks:
        for match in ANY_DECL_RE.finditer(chunk):
            field, name = match.group(1), match.group(2)
            primary.add(field)
            decls.append((field, name.replace(" ", ""), None))
        for match in re.finditer(r"(\w+)\s*=\s*(\w+)\s*(?:,|$)", chunk):
            alias, target = match.group(1), match.group(2)
            if alias in primary or target not in primary:
                continue
            if alias == target:
                continue
            decls.append((alias, alias, target))
    return decls


def resolve_mt_ref(ref: str, paths: dict[str, str]) -> str | None:
    ref = ref.strip()
    if not ref.startswith("MT."):
        return None
    tail = ref[3:]
    if tail.startswith("WOODS."):
        field = tail.split(".", 1)[1]
        if field == "Magic":
            return "GT6Materials.Woods.Magic"
        return paths.get(field)
    if tail.startswith("STONES."):
        field = tail.split(".", 1)[1]
        return paths.get(field)
    if tail in ("Ma", "Magic"):
        return "MT.Ma"
    return paths.get(tail)


def expand_any_ref(token: str, any_members: dict[str, list[str]]) -> list[str]:
    token = token.strip()
    match = re.match(r"ANY\.(\w+)\.mToThis", token)
    if not match:
        return []
    return list(any_members.get(match.group(1), []))


def parse_init_lines(text: str, paths: dict[str, str]) -> dict[str, dict]:
    init: dict[str, dict] = {}
    in_init = False
    for line in text.splitlines():
        if "protected static void init" in line or "static void init" in line:
            in_init = True
            continue
        if in_init and line.strip() == "}":
            break
        if not in_init:
            continue
        stripped = line.strip()
        if not stripped or stripped.startswith("MT.") or stripped.startswith("if "):
            continue
        if "." not in stripped:
            continue
        field_match = re.match(r"(\w+)\s*\.", stripped)
        if not field_match:
            continue
        field = field_match.group(1)
        if field in SKIP_INIT_FIELDS or field.startswith("_"):
            continue
        entry = init.setdefault(field, {"local": None, "members": []})
        local_match = LOCAL_RE.search(line)
        if local_match:
            entry["local"] = local_match.group(1)
        for rereg_match in REREG_RE.finditer(line):
            args = rereg_match.group(1)
            for token in args.split(","):
                token = token.strip()
                if not token or "ZL_MT" in token or ".toArray" in token:
                    continue
                if token.startswith("ANY."):
                    continue
                resolved = resolve_mt_ref(token, paths)
                if resolved:
                    entry["members"].append(resolved)
    return init


def expand_cross_any_members(init: dict[str, dict]) -> None:
    """Expand ANY.X.mToThis references once base members are known."""
    any_members = {field: data.get("members", []) for field, data in init.items()}

    gt6_text = GT6_ANY.read_text(encoding="utf-8", errors="ignore")
    in_init = False
    for line in gt6_text.splitlines():
        if "protected static void init" in line:
            in_init = True
            continue
        if in_init and line.strip() == "}":
            break
        if not in_init:
            continue
        field_match = re.match(r"\s*(\w+)\s*\.", line)
        if not field_match:
            continue
        field = field_match.group(1)
        if field not in init:
            continue
        for rereg_match in REREG_RE.finditer(line):
            args = rereg_match.group(1)
            for token in args.split(","):
                token = token.strip()
                expanded = expand_any_ref(token, any_members)
                for member in expanded:
                    if member not in init[field]["members"]:
                        init[field]["members"].append(member)


def emit_init(init: dict[str, dict]) -> list[str]:
    lines: list[str] = []
    for field in sorted(init.keys()):
        data = init[field]
        local = data.get("local")
        members: list[str] = []
        for member in data.get("members", []):
            if member not in members:
                members.append(member)
        if not local and not members:
            continue
        chain = field
        if local:
            chain += f'.setLocalName("{java_string(local)}")'
        if members:
            chain += ".addReRegistrationToThis(" + ", ".join(members) + ")"
        lines.append(f"        {chain};")
    return lines


def main() -> None:
    gt6_text = GT6_ANY.read_text(encoding="utf-8", errors="ignore")
    paths = load_material_paths(GT6_MATERIALS)
    decls = parse_declarations(gt6_text)
    init_data = parse_init_lines(gt6_text, paths)
    expand_cross_any_members(init_data)

    decl_lines: list[str] = []
    seen: set[str] = set()
    for field, registry, alias_target in decls:
        if field in seen:
            continue
        seen.add(field)
        if alias_target:
            decl_lines.append(f"            {field} = {alias_target},")
        else:
            decl_lines.append(f'            {field} = any("{java_string(registry)}"),')
    if decl_lines:
        decl_lines[-1] = decl_lines[-1].rstrip(",")

    # MT:208 diamond(...) puts all eight factory-created diamonds into ANY.Diamond.
    # The ANY.java-only parser cannot see these bindings; keep their source set explicit.
    diamonds = ["Diamond", "DiamondBlue", "DiamondGreen", "DiamondPurple", "DiamondRed",
                "DiamondYellow", "DiamondPink", "DiamondIndustrial"]
    diamond_members = init_data.setdefault("Diamond", {"members": []})["members"]
    for field in diamonds:
        member = f"GT6Materials.Compounds.{field}"
        if member not in diamond_members: diamond_members.append(member)
    init_lines = emit_init(init_data)
    needs_mt = any("MT.Ma" in line for line in init_lines)

    imports = (
        "import com.gregtech.gregtech.api.material.GTMaterial;\n"
        "import com.gregtech.gregtech.api.material.GTMaterialRegistry;\n"
        "import com.gregtech.gregtech.api.material.MaterialProperty;\n"
        "import com.gregtech.gregtech.data.generated.GT6Materials;\n"
    )
    if needs_mt:
        imports += "import com.gregtech.gregtech.data.ImportedMaterialData;\n"

    java = (
        "package com.gregtech.gregtech.data;\n\n"
        + imports
        + "\n"
        "/**\n"
        " * Generic \"Any X\" unification materials from GregTech 6 ANY.java.\n"
        " * Auto-transpiled — do not edit by hand.\n"
        " */\n"
        "public final class ANY {\n"
        "    private ANY() {}\n\n"
        "    private static boolean initialized = false;\n\n"
        "    private static GTMaterial any(String name) {\n"
        "        return GTMaterialRegistry.createMaterial(-1, name, name, 0xFFFFFF)\n"
        "                .put(MaterialProperty.HIDDEN);\n"
        "    }\n\n"
        "    public static final GTMaterial\n"
        + "\n".join(decl_lines)
        + ";\n\n"
        "    public static void init() {\n"
        "        if (initialized) {\n"
        "            return;\n"
        "        }\n"
        "        initialized = true;\n\n"
        + "\n".join(init_lines)
        + "\n    }\n"
        "}\n"
    )
    java = readable_java(java).replace("public final class MaterialGroups", "public class MaterialGroups").replace("private MaterialGroups()", "protected MaterialGroups()")
    OUT.write_text(java, encoding="utf-8")
    print(f"Wrote {OUT.name}: {len(decl_lines)} any-materials, {len(init_lines)} init bindings")


if __name__ == "__main__":
    main()
