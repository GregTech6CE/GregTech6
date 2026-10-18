"""Translate GT6's exact octagon masks; generated coordinates are relative to the controller."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import re
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/multiblocks/MultiTileEntityFusionReactor.java'

def cells():
    source = SOURCE.read_text(encoding='utf8').split('OCTAGONS =', 1)[1].split('};', 1)[0]
    rows = re.findall(r'\{([FT,]+)\}', source)
    assert len(rows) == 57
    result = {}
    def put(x, y, z, part, role='CASING'):
        position = (x, y, z + 2)
        if position == (0, 0, 0):
            return
        assert position not in result, position
        result[position] = (part, role)
    cpu = 0
    for x in range(-2, 3):
        for y in range(-2, 3):
            for z in range(-2, 3):
                radius = x*x+y*y+z*z
                if radius < 4:
                    put(x,y,z,18200 if cpu < 3 else 18201 if cpu < 15 else 18202)
                    cpu += 1
                elif radius > 6 or y == 0 and (abs(x) == 2 and z == 0 or abs(z) == 2 and x == 0):
                    put(x,y,z,18008)
                else:
                    put(x,y,z,18299)
    for distance in [3,4]:
        for x,z in [(-distance,0),(distance,0),(0,distance)]:
            put(x,0,z,18008)
    for mask in range(3):
        for x in range(19):
            for z, enabled in enumerate(rows[mask*19+x].split(',')):
                if enabled != 'T':
                    continue
                for y in ([-1,0,1] if mask == 0 else [-2,-1,0,1,2]):
                    role = 'CASING'
                    if mask == 0:
                        part = 18003
                        role = 'ITEM_FLUID_IO' if y else 'ENERGY_OUTPUT' if (x == 9 and z in [0,18] or z == 9 and x in [0,18]) else 'ENERGY_INPUT'
                    elif mask == 1:
                        part = 18045 if y == 0 else 18003
                        if abs(y) == 2: role = 'ITEM_FLUID_IO'
                    else:
                        part = 18002 if y == 0 else 18045 if abs(y) == 1 else 18003
                        if abs(y) == 2: role = 'ITEM_FLUID_IO'
                    put(x-9,y,z-9,part,role)
    assert Counter(p for p,r in result.values()) == {18003:576,18045:144,18299:50,18002:36,18008:53,18200:3,18201:12,18202:12}
    return result

def main():
    entries = cells()
    text = '''package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import java.util.List;

/** Generated from GT6 MultiTileEntityFusionReactor by tools/import_fusion_structure.py.
 * CPU coordinates in this list are an assembly example; validation accepts all permutations. */
public final class FusionStructure {
    private FusionStructure() {}
    public static final List<LargeMachineLayouts.Cell> CELLS = List.of(
'''
    text += ',\n'.join(f'        new LargeMachineLayouts.Cell({x},{y},{z},{part},Role.{role})' for (x,y,z),(part,role) in entries.items())
    text += '\n    );\n}\n'
    (native_path(ROOT / 'src/main/java/com/gregtech/gregtech/content/multiblock/FusionStructure.java')).write_text(text,encoding='utf8')
    print(f'Fusion: {len(entries)} parts plus controller')

if __name__ == '__main__':
    main()
