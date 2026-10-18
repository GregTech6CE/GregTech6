#!/usr/bin/env python3
"""Rename material_icons -> material_icons and lowercase all texture paths."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")


def case_insensitive_rename(path: Path, new_name: str) -> Path:
    target = path.parent / new_name
    if path.name == new_name:
        return path
    if path.exists():
        if target.exists() and path.resolve() != target.resolve():
            raise FileExistsError(f"Cannot rename {path} -> {target}")
        temp = path.parent / (f"__tmp__{new_name}")
        path.rename(temp)
        temp.rename(target)
        return target
    return target


def lowercase_tree(root: Path) -> None:
    if not root.exists():
        return
    for dirpath, dirnames, filenames in os.walk(root, topdown=False):
        dp = Path(dirpath)
        for name in filenames:
            src = dp / name
            lower = name.lower()
            if name != lower:
                case_insensitive_rename(src, lower)
        for name in dirnames:
            src = dp / name
            lower = name.lower()
            if name != lower:
                case_insensitive_rename(src, lower)


def rename_top_dirs() -> None:
    for kind in ("item", "block"):
        old = ASSETS / "textures" / kind / "material_icons"
        new = ASSETS / "textures" / kind / "material_icons"
        if old.exists():
            if new.exists():
                raise FileExistsError(f"{new} already exists")
            old.rename(new)
            print(f"Renamed textures/{kind}/material_icons -> material_icons")


def patch_text(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    updated = text.replace("material_icons", "material_icons")
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        return True
    return False


def patch_json_overlay_paths(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    updated = re.sub(r"_OVERLAY\b", "_overlay", text)
    updated = re.sub(r"_overlay_overlay", "_overlay", updated)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        return True
    return False


def main() -> None:
    rename_top_dirs()
    for kind in ("item", "block"):
        lowercase_tree(ASSETS / "textures" / kind / "material_icons")
    lowercase_tree(ASSETS / "textures" / "block" / "stones")

    patched = 0
    for base in (
        native_path(ROOT / "src/main/java"),
        native_path(ROOT / "src/main/resources"),
        ROOT / "tools",
    ):
        for path in base.rglob("*"):
            if not path.is_file():
                continue
            if path.suffix.lower() not in {".java", ".json", ".py", ".mcmeta"}:
                continue
            if patch_text(path):
                patched += 1
            if path.suffix.lower() == ".json":
                if patch_json_overlay_paths(path):
                    patched += 1

    print(f"Patched {patched} files")


if __name__ == "__main__":
    main()
