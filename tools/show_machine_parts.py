"""Report the port's multiblock part table (ids and names)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import re

PATH = str(native_path("src/main/java/com/gregtech/gregtech/content/multiblock/LargeMachineParts.java"))
text = io.open(PATH, encoding="utf-8", errors="replace").read()
parts = re.findall(r'new Part\((\d+),\s*"([a-z0-9_]+)"', text)
print("parts now:", len(parts))
for pid, name in parts:
    if int(pid) >= 17000:
        print("   %-6s %s" % (pid, name))
