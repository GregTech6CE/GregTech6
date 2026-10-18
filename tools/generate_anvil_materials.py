"""Import the original, uncoated self-forging metal definitions for anvil joining/flattening."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java/gregapi/data/MT.java'
OUT = native_path(ROOT / 'src/main/java/com/gregtech/gregtech/content/tool/AnvilMaterialCatalog.java')

def names(text):
    result = set()
    factories = {'metal', 'metalloid', 'metal_'}
    edges = re.findall(r'static\s+OreDictMaterial\s+(\w+)\s*\([^\n]+?\{return\s+(\w+)\s*\(', text)
    while True:
        expanded = factories | {name for name, parent in edges if parent in factories}
        if expanded == factories:
            break
        factories = expanded
    for line in text.splitlines():
        match = re.search(r'(?:=|return)\s*(\w+)\s*\(\s*\d+\s*,\s*"([^"]+)"', line)
        if not match or match[1] not in factories:
            continue
        # These declarations change the default (self, U) forging target or prohibit this handler.
        if re.search(r'\b(?:FLAMMABLE|COATED|ANTIMATTER)\b|\.setForging\(|\.setAllToTheOutputOf\(', line):
            continue
        result.add(match[2].replace(' ', '').replace('-', '').replace("'", ''))
    return sorted(result)

if __name__ == '__main__':
    materials = names(SOURCE.read_text(encoding='utf8'))
    rows = ',\n'.join('        "' + name + '"' for name in materials)
    OUT.write_text('''package com.gregtech.gregtech.content.tool;

/** Generated from GT6 MT.java by tools/generate_anvil_materials.py.
 * Special forging conversions are deliberately separate from the self-forging rules. */
public final class AnvilMaterialCatalog {
    private AnvilMaterialCatalog() {}
    public static final java.util.Set<String> SELF_FORGING = java.util.Set.of(
''' + rows + ');\n}\n', encoding='utf8')
    print(f'Imported {len(materials)} original self-forging metal definitions')
