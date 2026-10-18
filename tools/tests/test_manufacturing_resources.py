
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json,unittest,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check_render_resources import check
class ManufacturingResources(unittest.TestCase):
    def test_molds_reference_real_items_and_models(self):
        recipes=list((native_path(ROOT/'src/main/resources/data/gt6ce/recipes/extruder_shapes')).glob('*.json'))
        self.assertEqual(64,len(recipes))
        ids=set()
        for path in recipes:
            data=json.loads(path.read_text(encoding='utf8'))
            self.assertFalse(data['allow_mirror'])
            ids.add(data['result']['item'].split(':')[1])
            for value in data['key'].values():
                name=value['item'].split(':')[1]
                if not name.startswith('plate_'):ids.add(name)
        issues,_=check([native_path(ROOT/'src/main/resources/assets/gt6ce/models/item'/f'{name}.json') for name in ids])
        self.assertEqual([],issues)
    def test_wire_resources_conserve_width_and_material(self):
        recipes=list((native_path(ROOT/'src/main/resources/data/gt6ce/recipes/wire_working')).glob('*.json'))
        self.assertEqual(1316,len(recipes))
        for path in recipes:
            d=json.loads(path.read_text(encoding='utf8'));output=d['result']['item'].split(':')[1].split('_',2)
            total=0
            for ingredient in d['ingredients']:
                if 'tag' in ingredient:
                    self.assertEqual(ingredient, {'tag': 'forge:plates/rubber'})
                    continue
                name=ingredient['item'].split(':')[1]
                if name=='plate_rubber':continue
                wire=name.split('_',2);self.assertEqual(wire[2],output[2]);total+=int(wire[1])
            self.assertEqual(total,int(output[1])*d['result']['count'])
            self.assertLessEqual(len(d['ingredients']),9)
    def test_missing_low_heat_small_gear_uses_original_asset(self):
        assets=native_path(ROOT/'src/main/resources/assets/gt6ce')
        original=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/items/gt.multiitem.technological/10226.png'
        self.assertEqual(original.read_bytes(),(assets/'textures/item/technological/low_heat_extruder_shape_smallgear.png').read_bytes())
        for lang in ['en_us','zh_cn']:
            self.assertTrue(json.loads((assets/'lang'/f'{lang}.json').read_text(encoding='utf8'))['item.gt6ce.low_heat_extruder_shape_smallgear'])
