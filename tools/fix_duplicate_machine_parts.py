"""Drop LargeMachineParts entries whose name is already registered by GTMultiblocks.

The 18xxx part band overlaps the multiblock controllers' own part blocks
(`GTMultiblocks.CENTRIFUGE_PART`, `HEAT_TRANSMITTER`, ...), which are registered under the same
block ids; keeping both would be a duplicate registration.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import re

PARTS = str(native_path("src/main/java/com/gregtech/gregtech/content/multiblock/LargeMachineParts.java"))
MULTIBLOCKS = str(native_path("src/main/java/com/gregtech/gregtech/registry/GTMultiblocks.java"))

parts = io.open(PARTS, encoding="utf-8").read()
mblocks = io.open(MULTIBLOCKS, encoding="utf-8").read()

# Names GTMultiblocks registers (reg("name", ...) or block("name", ...)).
blocked = set(re.findall(r'reg\w*\(\s*"([a-z0-9_]+)"', mblocks))
blocked |= set(re.findall(r'block\(\s*"([a-z0-9_]+)"', mblocks))
# GTMultiblocks also names its part blocks through other helpers; any id-looking literal counts.
blocked |= set(re.findall(r'"([a-z][a-z0-9_]{3,})"', mblocks))
# Ids the non-throwing switch in find(...) already maps to a hosted block.
switch_ids = {int(m) for m in re.findall(r'case\s+(\d+)\s*->', parts)}
print("names owned by GTMultiblocks: %d, ids handled by find(): %d" % (len(blocked), len(switch_ids)))

lines = parts.splitlines(keepends=True)
kept, removed = [], []
seen = set()
for line in lines:
    match = re.match(r'\s*new Part\((\d+),\s*"([a-z0-9_]+)"', line)
    if match:
        pid, name = match.group(1), match.group(2)
        if int(pid) in switch_ids or name in blocked or name in seen:
            why = "find() switch" if int(pid) in switch_ids else (
                "GTMultiblocks" if name in blocked else "duplicate")
            removed.append((pid, name, why))
            continue
        seen.add(name)
    kept.append(line)

if removed:
    io.open(PARTS, "w", encoding="utf-8", newline="\n").write("".join(kept))
    print("removed %d entries:" % len(removed))
    for pid, name, why in removed:
        print("   %-6s %-34s %s" % (pid, name, why))
else:
    print("nothing to remove")
print("parts left: %d" % len(re.findall(r'new Part\(', "".join(kept))))
