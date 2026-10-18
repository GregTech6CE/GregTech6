
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ASSETS=native_path(ROOT/'src/main/resources/assets/gt6ce')
class TransformerAssets(unittest.TestCase):
    def test_nine_transformers_three_layers_all_faces_and_water_states(self):
        steps=['ulv_lv','lv_mv','mv_hv','hv_ev','ev_iv','iv_luv','luv_zpm','zpm_uv','uv_xv']
        for step in steps:
            states=json.loads((ASSETS/f'blockstates/transformer_{step}.json').read_text(encoding='utf-8'))['variants']
            self.assertEqual(len(states),36)
            for state,entry in states.items():
                activity=int(state.split('activity=')[1].split(',')[0])
                model=json.loads((ASSETS/('models/'+entry['model'].split(':')[1]+'.json')).read_text(encoding='utf-8'))
                layers=model['children'];overlay=['overlay','overlay_active','overlay_blinking'][activity]
                self.assertTrue(all('/'+overlay+'/' in t for t in layers['layer1']['textures'].values()))
                self.assertTrue(all('tintindex' not in f for f in layers['layer1']['elements'][0]['faces'].values()))
                self.assertTrue(all(f.get('tintindex')==0 for f in layers['layer0']['elements'][0]['faces'].values()))
    def test_original_transformer_texture_and_animation_bytes(self):
        original=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/machines/transformers/transformer_electric'
        copied=ASSETS/'textures/block/machines/transformers/transformer_electric'
        sources=[p for p in original.rglob('*') if p.is_file()]
        self.assertGreaterEqual(len(sources),12)
        for p in sources:self.assertEqual(p.read_bytes(),(copied/p.relative_to(original)).read_bytes())
if __name__=='__main__':unittest.main()
