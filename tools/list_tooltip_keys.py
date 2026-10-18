"""List shared tooltip keys available in the port's language file (for the bookshelf tooltip)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import re

text = io.open(str(native_path("src/main/resources/assets/gt6ce/lang/en_us.json")), encoding="utf-8",
               errors="replace").read()
keys = re.findall(r'"(tooltip\.gt6ce\.[a-zA-Z0-9_.]+)":\s*"([^"]*)"', text)
for key, value in keys:
    if any(word in key.lower() for word in ("click", "gui", "magnif", "pincer", "interact")):
        print("%-58s %s" % (key, value[:70]))
print("--- total tooltip keys:", len(keys))
