"""Add the language keys for the valve/barometer parts (registered names must have translations)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os
import re

GT6 = (r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregtech\loaders\b"
       r"\Loader_MultiTileEntities.java")
LANG = os.path.join(str(native_path("src")), "main", "resources", "assets", "gt6ce", "lang")

WANTED = set(p for p in range(17001, 18400) if not (17100 <= p < 17200))

flat = re.sub(r"\s+", " ", io.open(GT6, encoding="utf-8", errors="replace").read())
names = {}
for match in re.finditer(r'aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"\s*,\s*(\d+)', flat):
    pid = int(match.group(2))
    if pid in WANTED:
        names[pid] = match.group(1)


def snake(name):
    return re.sub(r"__+", "_", re.sub(r"[^A-Za-z0-9]+", "_", name).strip("_").lower())


entries = {("block.gt6ce." + snake(name)): name for name in names.values()}
print("lang keys to add: %d" % len(entries))

for filename in ("en_us.json", "zh_cn.json"):
    path = os.path.join(LANG, filename)
    lines = io.open(path, encoding="utf-8").readlines()
    anchor = next((i for i, line in enumerate(lines) if '"block.gt6ce.loot_crate"' in line), None)
    if anchor is None:
        raise SystemExit("%s: no anchor" % filename)
    indent = lines[anchor][:len(lines[anchor]) - len(lines[anchor].lstrip())]
    inserts = []
    for key, value in entries.items():
        if any(('"%s"' % key) in line for line in lines):
            continue
        inserts.append('%s"%s": "%s",\n' % (indent, key, value))
    lines[anchor:anchor] = inserts
    io.open(path, "w", encoding="utf-8", newline="\n").writelines(lines)
    print("%s: %d keys" % (filename, len(inserts)))
