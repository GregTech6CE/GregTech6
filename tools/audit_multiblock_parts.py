"""Audit GT6's "Multiblock Machines" part registrations against the port's part blocks.

GT6 registers every wall/coil/valve/etc. as a multi-tile in the "Multiblock Machines" group with a
numeric id (18000+). The port hosts them either in LargeMachineParts.DEFINITIONS or through the
non-throwing switch in LargeMachineParts.find(...).
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import re

GT6 = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregtech\loaders\b\Loader_MultiTileEntities.java"
PORT_PARTS = str(native_path("src/main/java/com/gregtech/gregtech/content/multiblock/LargeMachineParts.java"))

text = io.open(GT6, encoding="utf-8", errors="replace").read()
flat = re.sub(r"\s+", " ", text)

parts = []
for match in re.finditer(r'aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"\s*,\s*(\d+)\s*,', flat):
    parts.append((int(match.group(2)), match.group(1)))
for match in re.finditer(r'aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"[^,]*,\s*(\d+)\s*,', flat):
    entry = (int(match.group(2)), match.group(1))
    if entry not in parts:
        parts.append(entry)

# Materials assigned right before each registration (aMat = ...; then aRegistry.add(...)).
materials = {}
for match in re.finditer(r'aMat\s*=\s*([^;]+);\s*aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"\s*,\s*(\d+)', flat):
    materials[int(match.group(3))] = match.group(1).strip()

port = io.open(PORT_PARTS, encoding="utf-8", errors="replace").read()
port_ids = {int(m) for m in re.findall(r'new Part\((\d+),', port)}
port_ids |= {int(m) for m in re.findall(r'case (\d+)\s*->', port)}
port_names = set(re.findall(r'new Part\(\d+,\s*"([a-z0-9_]+)"', port))

parts.sort()
print("GT6 multiblock parts: %d" % len(parts))
missing = [(i, n) for i, n in parts if i not in port_ids]
print("port covers %d, missing %d" % (len(parts) - len(missing), len(missing)))
print()
print("%-7s %-42s %-22s %s" % ("id", "GT6 name", "material", "in port"))
for pid, name in parts:
    mark = "yes" if pid in port_ids else "*** MISSING ***"
    print("%-7d %-42s %-22s %s" % (pid, name, materials.get(pid, ""), mark))
