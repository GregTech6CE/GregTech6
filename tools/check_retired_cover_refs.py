"""Fail loudly when a shipped recipe still references one of the retired cover items."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json
import pathlib

RETIRED = ("gregtech:cover_pump", "gregtech:cover_conveyor", "gregtech:cover_robot_arm",
           "gregtech:cover_redstone_torch", "gregtech:cover_redstone_repeater",
           "gregtech:cover_tag_selector")

hits = []
for path in native_path(pathlib.Path("src/main/resources/data")).rglob("*.json"):
    text = path.read_text(encoding="utf-8", errors="replace")
    for name in RETIRED:
        if name in text:
            hits.append(f"{path}: {name}")
print("\n".join(hits) if hits else "no recipe references a retired cover item")
