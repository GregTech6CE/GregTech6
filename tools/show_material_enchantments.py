"""Print a few material enchantment rows from the generated table."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import re

text = io.open(str(native_path("src/main/java/com/gregtech/gregtech/loaders/c/GTEnchantmentTable.java")),
               encoding="utf-8", errors="replace").read()
for name in ["Diamond", "Gold", "Plastic", "Iron", "Ruby", "Thaumium"]:
    match = re.search(r'new Material\("' + name + r'", List\.of\(([^;]*?)\)\),', text)
    if not match:
        print("%-10s -> not in table" % name)
        continue
    print("%-10s -> %s" % (name, match.group(1)[:300]))
