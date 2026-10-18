"""Add GT6's tank valve and boiler barometer parts to the port's LargeMachineParts list.

GT6 registers them as "Multiblock Machines" multis with their own ids (17001-17067 valves,
17201-17205 boiler barometers); the port keeps a table of such parts in
`content/multiblock/LargeMachineParts.java` (`Part(originalId, name, material)`).
"""

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
PORT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "multiblock",
                    "LargeMachineParts.java")

# GT6 expression -> the port's material name (the field is only used for naming/texturing).
ALIASES = {
    "MT.StainlessSteel": "StainlessSteel",
    "MT.TungstenSteel": "TungstenSteel",
    "ANY.W": "Tungsten",
    "MT.Ad": "Adamantium",
    "MT.Ti": "Titanium",
    "MT.Invar": "Invar",
    "MT.WoodTreated": "WoodTreated",
    "MT.Pb": "Lead",
    "MT.Ceramic": "Ceramic",
    "MT.SteelGalvanized": "SteelGalvanized",
    "ANY.Steel": "Steel",
    "MT.InvarAlloy": "Invar",
}

WANTED = [p for p in range(17001, 18400)
          if not (17100 <= p < 17200)]  # the 171xx ids are the multiblock controllers themselves

text = io.open(GT6, encoding="utf-8", errors="replace").read()
flat = re.sub(r"\s+", " ", text)

# Each registration: aMat = ...; aRegistry.add("Name", "Multiblock Machines", id, ...)
parts = {}
for match in re.finditer(
        r'aMat\s*=\s*([A-Za-z0-9_.]+);\s*aRegistry\.add\("([^"]+)"\s*,\s*"Multiblock Machines"\s*,\s*(\d+)',
        flat):
    material, name, pid = match.group(1), match.group(2), int(match.group(3))
    if pid in WANTED:
        parts[pid] = (name, material)

print("found %d of %d wanted parts" % (len(parts), len(WANTED)))
missing = [p for p in WANTED if p not in parts]
if missing:
    print("missing ids:", missing)


def snake(name):
    cleaned = re.sub(r"[^A-Za-z0-9]+", "_", name).strip("_").lower()
    return re.sub(r"__+", "_", cleaned)


port = io.open(PORT, encoding="utf-8").read()
existing = {int(m) for m in re.findall(r'new Part\((\d+),', port)}
# Ids the find() switch already maps to a hosted block (GTMultiblocks owns those).
existing |= {int(m) for m in re.findall(r'case\s+(\d+)\s*->', port)}
existing_names = set(re.findall(r'new Part\(\d+,\s*"([a-z0-9_]+)"', port))
additions = []
for pid in sorted(parts):
    if pid in existing:
        continue
    name, material = parts[pid]
    snake_name = snake(name)
    if snake_name in existing_names:
        # Another GT6 id already registered this part name; keep one block per name.
        continue
    existing_names.add(snake_name)
    additions.append('            new Part(%d,"%s","%s"),' % (pid, snake_name, ALIASES.get(material, material.split(".")[-1])))

if not additions:
    print("LargeMachineParts: already complete")
else:
    anchor = "            new Part(18108,\"shredder_blades\",\"TungstenSteel\"));"
    assert port.count(anchor) == 1, "definition tail anchor"
    port = port.replace(anchor, "\n".join(additions) + "\n" +
                        "            new Part(18108,\"shredder_blades\",\"TungstenSteel\"));")
    io.open(PORT, "w", encoding="utf-8", newline="\n").write(port)
    print("LargeMachineParts: added %d parts" % len(additions))
    for line in additions:
        print("   ", line.strip())
