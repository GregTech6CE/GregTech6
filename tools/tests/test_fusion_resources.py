
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import re
import unittest
from pathlib import Path
from collections import Counter

ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gt6ce')


class FusionResources(unittest.TestCase):
    def test_exact_original_part_counts_and_energy_ports(self):
        source = (native_path(ROOT / 'src/main/java/com/gregtech/gregtech/content/multiblock/FusionStructure.java')).read_text(encoding='utf8')
        cells = re.findall(r'Cell\((-?\d+),(-?\d+),(-?\d+),(\d+),Role\.(\w+)\)', source)
        self.assertEqual(len(cells), 886)
        self.assertEqual(len({(x,y,z) for x,y,z,_,_ in cells}),886)
        self.assertEqual(Counter(int(part) for _,_,_,part,_ in cells), {18003:576,18045:144,18299:50,18002:36,18008:53,18200:3,18201:12,18202:12})
        self.assertEqual({(int(x),int(y),int(z)) for x,y,z,_,r in cells if r=='ENERGY_OUTPUT'}, {(-9,0,2),(9,0,2),(0,0,-7),(0,0,11)})

    def test_part_models_use_existing_original_textures_and_separate_tint(self):
        for path in (ASSETS/'models/block/machine/fusion').glob('*.json'):
            model=json.loads(path.read_text(encoding='utf8'))
            for name,child in model['children'].items():
                for texture in child['textures'].values():
                    self.assertTrue((ASSETS/('textures/'+texture.split(':')[1]+'.png')).is_file(),texture)
                for element in child['elements']:
                    for face in element['faces'].values():
                        self.assertEqual('tintindex' in face,name=='base')
        states=json.loads((ASSETS/'blockstates/fusion_reactor_main.json').read_text(encoding='utf8'))
        self.assertEqual(len(states['variants']),16)

    def test_source_reaction_values_are_preserved(self):
        original=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/c/Loader_Recipes_Other.java'
        if not original.exists():self.skipTest('Original GT6 checkout unavailable')
        source=original.read_text(encoding='utf8')
        expected=[]
        for line in source.splitlines():
            if 'RM.Fusion.addRecipe1' not in line:continue
            eut,ticks,circuit=re.search(r'addRecipe1\(F,\s*(-?\d+),\s*(\d+), ST.tag\((\d+)\)',line).groups()
            value=1
            for number in re.search(r'setSpecialNumber\(([^)]+)',line).group(1).replace('L','').split('*'):value*=int(number)
            expected.append((int(circuit),int(ticks),int(eut),value))
        port=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/content/recipe/Loader_Recipes_Fusion.java')).read_text(encoding='utf8')
        actual=[tuple(map(int,match)) for match in re.findall(r'add\((\d+),(\d+),(-?\d+),(\d+)L,',port)]
        self.assertEqual(actual,expected)
