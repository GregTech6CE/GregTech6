"""Find alloying recipes in the port's generated chem tables (which materials are alloyed from what)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import re

PATTERNS = ["Bronze", "Brass", "Steel", "Invar", "Electrum", "Cupronickel", "Solder"]

for path in sorted(glob.glob(str(_NativePath(__file__).resolve().parents[1] / "core/src/main/java/com/gregtech/gregtech/content/recipe/generated/*Gen.java"))):
    text = io.open(path, encoding="utf-8", errors="replace").read()
    if 'CrucibleAlloying' not in text and 'Alloy' not in text:
        continue
    print("==", path.replace("\\", "/"))
    for line in text.splitlines():
        if "CrucibleAlloying" in line and any(p in line for p in PATTERNS):
            print("   ", line.strip()[:200])
