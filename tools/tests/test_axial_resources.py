
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check_render_resources import check

class AxialResources(unittest.TestCase):
    def test_all_graded_controllers_resolve_to_original_colored_models(self):
        assets=native_path(ROOT/'src/main/resources/assets/gt6ce')
        names=['large_turbine_main','large_steam_turbine_trinitanium','large_steam_turbine_graphene','large_steam_turbine_vibramantium',
               'large_dynamo_main','large_dynamo_titanium','large_dynamo_tungstensteel','large_dynamo_adamantium']
        paths=[assets/f'{folder}/{name}.json' for folder in ['blockstates','models/item'] for name in names]
        issues,_=check(paths)
        self.assertEqual([],issues)
        for family in ['large_turbine_main','large_dynamo_main']:
            model=json.loads((assets/f'models/block/machine/multiblock/{family}.json').read_text())
            faces=model['children']['colored']['elements'][0]['faces']
            self.assertEqual(6,len(faces))
            self.assertTrue(all(face['tintindex']==0 for face in faces.values()))
    def test_axial_instructions_have_matching_translations(self):
        assets=native_path(ROOT/'src/main/resources/assets/gt6ce/lang')
        en=json.loads((assets/'en_us.json').read_text(encoding='utf8'))
        zh=json.loads((assets/'zh_cn.json').read_text(encoding='utf8'))
        for key in ['gt.tooltip.axial.steam','gt.tooltip.axial.dynamo','gt.tooltip.axial.control','gt.axial.state.enabled',
                    'gt.axial.state.stopped','gt.axial.state.overloaded','gt6ce.jei.assembly_steam_turbine','gt6ce.jei.assembly_dynamo']:
            self.assertTrue(en[key]);self.assertTrue(zh[key]);self.assertEqual(en[key].count('%s'),zh[key].count('%s'))
