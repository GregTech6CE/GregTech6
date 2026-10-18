"""Find the port's water-family fluid ids (GT6 FL.Ocean / FL.River / FL.Swamp and friends)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import re

PATTERNS = ("river", "sea", "swamp", "ocean", "water")

hits = set()
for path in glob.glob(str(native_path("src/main/java/com/gregtech/gregtech/**/*.java")), recursive=True):
    text = io.open(path, encoding="utf-8", errors="replace").read()
    for match in re.finditer(r'fluid\(\s*"([A-Za-z0-9_]+)"', text):
        name = match.group(1)
        if any(key in name.lower() for key in PATTERNS):
            hits.add((path.replace("\\", "/").split("/")[-1], name))
for entry in sorted(hits):
    print("%-34s %s" % entry)
