"""Cross-check which materials of the generated enchantment table exist in the port.

The table's rows are the receiver of GT6's `addEnchantmentFor…` calls, i.e. GT6 **field** names
(`Ma`, `Fe`, `PO4`, `Polycarbonate`), not material names. They are resolved through the generated
field table (`GTMaterialFields`) and then matched against the port's own material registry names,
which the language file stores without spaces (`Hard Plastic` -> `material.gt6ce.hardplastic`).

Rows that name a material this port does not have are other mods' materials GT6 enchanted for
compatibility; they are listed separately so a real regression (a port material losing its row)
stands out.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import re

TABLE = str(native_path("src/main/java/com/gregtech/gregtech/loaders/c/GTEnchantmentTable.java"))
FIELDS = str(native_path("src/main/java/com/gregtech/gregtech/loaders/c/GTMaterialFields.java"))
LANG = str(native_path("src/main/resources/assets/gt6ce/lang/zh_cn.json"))


def norm(name: str) -> str:
    """The port's material key spelling: lower case, no spaces or punctuation."""
    return re.sub(r"[^a-z0-9]", "", name.lower())


# The port imported GT6's own language file, so a few `material.gt6ce.*` keys name materials the
# port does not register (their rows stay unresolved at runtime too).
INHERITED_ONLY = {"sunstone"}

port_names = {norm(key.split(".", 2)[2]) for key in json.load(io.open(LANG, encoding="utf-8"))
              if key.startswith("material.gt6ce.")} - INHERITED_ONLY
fields = dict(re.findall(r'BY_FIELD\.put\("([^"]+)", "([^"]+)"\);',
                         io.open(FIELDS, encoding="utf-8").read()))
rows = re.findall(r'new Material\("(\w+)", List\.of\(',
                  io.open(TABLE, encoding="utf-8").read())

resolved, unresolved = [], []
for row in rows:
    name = fields.get(row, row)
    (resolved if norm(name) in port_names else unresolved).append((row, name))

print("table rows: %d, field table: %d, port materials: %d"
      % (len(rows), len(fields), len(port_names)))
print("resolved to a port material: %d" % len(resolved))
print("rows pointing at a material the port does not have: %d" % len(unresolved))
for row, name in unresolved:
    print("   %-20s -> %s" % (row, name))
print()
print("rows that only resolve through GT6's field table (short or differently spelled names):")
short = [(row, name) for row, name in resolved if row != name]
for row, name in short[:20]:
    print("   %-20s -> %s" % (row, name))
print("   total: %d" % len(short))
