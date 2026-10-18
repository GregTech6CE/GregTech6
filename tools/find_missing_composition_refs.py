
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
comp = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/generated/MaterialCompositionData.java")).read_text(encoding="utf-8")
refs = set(re.findall(r'get\("([^"]+)"\)', comp))
pattern = r"^\s+(\w+(?:\s*,\s*\w+)*)\s*=\s*(?:\w+\.)?(?:create|metal|element|alloy|ore|gem|dust|gas|wood|stone|elec|cent|clay)\("
fields = set()
for rel in (
    "tools/generated/materials-source.java.txt",
    str(native_path("src/main/java/com/gregtech/gregtech/data/ImportedMaterialData.java")),
    str(native_path("src/main/java/com/gregtech/gregtech/data/AntimatterMaterials.java")),
):
    source = (ROOT / rel).read_text(encoding="utf-8")
    for match in re.finditer(pattern, source, re.M):
        for part in match.group(1).split(","):
            fields.add(part.strip())
fields |= {"Ma", "Empty", "NULL"}
missing = sorted(r for r in refs if r not in fields)
print("missing count", len(missing))
for name in missing:
    print(name)
