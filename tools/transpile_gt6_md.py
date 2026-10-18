#!/usr/bin/env python3
"""Transpile GregTech 6 MD.java + CS.ModIDs into GregTech6 MD.java."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
from pathlib import Path
from readable_data_names import readable_java

GT6_MD = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\MD.java")
GT6_CS = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data\CS.java")
OUT = native_path(Path(__file__).resolve().parents[1] / "src/main/java/com/gregtech/gregtech/data/ModReferences.java")

MODID_RE = re.compile(r"^\s*,?\s*(\w+)\s*=\s*\"((?:\\.|[^\"\\])*)\"")
MD_ENTRY_RE = re.compile(
    r"^\s*(?:public static final ModData\s+)?,?\s*(\w+)\s*=\s*new ModData\((?:ModIDs\.)?(\w+|\"(?:\\.|[^\"\\])*\")\s*,\s*\"((?:\\.|[^\"\\])*)\""
)


def load_mod_ids(cs_text: str) -> dict[str, str]:
    ids: dict[str, str] = {}
    in_modids = False
    for line in cs_text.splitlines():
        if "class ModIDs" in line:
            in_modids = True
            continue
        if in_modids and line.strip().startswith("}"):
            break
        if not in_modids:
            continue
        match = MODID_RE.match(line)
        if match:
            ids[match.group(1)] = match.group(2)
    return ids


def java_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def resolve_mod_id(token: str, mod_ids: dict[str, str]) -> str:
    if token.startswith('"') and token.endswith('"'):
        return bytes(token[1:-1], "utf-8").decode("unicode_escape")
    return mod_ids.get(token, token.lower())


def main() -> None:
    mod_ids = load_mod_ids(GT6_CS.read_text(encoding="utf-8", errors="ignore"))
    entries: list[tuple[str, str, str]] = []
    for line in GT6_MD.read_text(encoding="utf-8", errors="ignore").splitlines():
        match = MD_ENTRY_RE.match(line)
        if not match:
            continue
        field, id_token, display = match.group(1), match.group(2), match.group(3)
        if field == "UNKNOWN":
            continue
        mod_id = resolve_mod_id(id_token, mod_ids)
        if field == "GT":
            mod_id = "gt6ce"  # matches GregTech.MODID
        entries.append((field, mod_id, display))

    body_lines = [
        f'            {field} = new ModData("{java_string(mod_id)}", "{java_string(display)}"),'
        for field, mod_id, display in entries
    ]
    if body_lines:
        body_lines[-1] = body_lines[-1].rstrip(",")

    java = (
        "package com.gregtech.gregtech.data;\n\n"
        "import com.gregtech.gregtech.GregTech;\n"
        "import com.gregtech.gregtech.api.mod.ModData;\n\n"
        "/**\n"
        " * Known mod IDs for material provenance and unification.\n"
        " * Auto-transpiled from GregTech 6 MD.java + CS.ModIDs. Do not edit by hand.\n"
        " */\n"
        "public final class MD {\n"
        "    private MD() {}\n\n"
        "    public static final ModData UNKNOWN = new ModData(\"unknown\", \"Unknown Mod\").setLoaded(false);\n"
        "    static {\n"
        "        ModData.MODS.put(null, UNKNOWN);\n"
        "    }\n\n"
        "    public static final ModData\n"
        + "\n".join(body_lines)
        + ";\n"
        "}\n"
    )
    java = readable_java(java).replace("public final class ModReferences", "public class ModReferences").replace("private ModReferences()", "protected ModReferences()")
    OUT.write_text(java, encoding="utf-8")
    print(f"Wrote {OUT.name}: {len(entries)} mod entries")


if __name__ == "__main__":
    main()
