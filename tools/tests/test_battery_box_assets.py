import hashlib
import importlib.util
import json
import re
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS=ROOT/'src/main/resources/assets/gregtech'
TIERS=['ulv','lv','mv','hv','ev','iv','luv','zpm','uv','xv']
class BatteryBoxAssets(unittest.TestCase):
    def test_all_faces_water_states_and_three_original_layers(self):
        for ident in [prefix+t for prefix in ['battery_box_','energy_storage_'] for t in TIERS]:
            variants=json.loads((SHARED_ASSETS/f'blockstates/{ident}.json').read_text(encoding='utf-8'))['variants']
            self.assertEqual(len(variants),36)
            for state in range(3):
                for face in ['north','east','south','west','up','down']:
                    for water in ['false','true']:
                        ref=variants[f'facing={face},charge_state={state},waterlogged={water}']['model']
                        model=json.loads((SHARED_ASSETS/('models/'+ref.split(':')[1]+'.json')).read_text(encoding='utf-8'))
                        layers=model['children']
                        self.assertTrue(all(f['tintindex']==0 for f in layers['layer0']['elements'][0]['faces'].values()))
                        self.assertTrue(all('tintindex' not in f for f in layers['layer1']['elements'][0]['faces'].values()))
                        overlay=['overlay','overlay_active','overlay_blinking'][state]
                        self.assertTrue(all('/'+overlay+'/' in t for t in layers['layer1']['textures'].values()))
                        self.assertEqual(len(layers['layer1']['elements'][0]['faces']),6)
    def test_original_animation_bytes_and_chinese_names(self):
        original=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/machines/energystorages/battery_electric'
        port=ASSETS/'textures/block/machines/energystorages/battery_electric'
        files=list(original.rglob('*'))
        self.assertEqual(sum(f.is_file() for f in files),10)
        for source in files:
            if source.is_file():self.assertEqual(source.read_bytes(),(port/source.relative_to(original)).read_bytes())
        lang=json.loads((ASSETS/'lang/zh_cn.json').read_text(encoding='utf-8'))
        cn=(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8')
        for i,tier in enumerate(TIERS):
            expected=re.search(r'^\s*S:gt\.multitileentity\.'+str(10080+i)+r'=(.*)$',cn,re.M).group(1).strip()
            self.assertEqual(lang['block.gregtech.battery_box_'+tier],expected)
    def test_selective_generator_is_idempotent_and_preserves_other_language_keys(self):
        spec=importlib.util.spec_from_file_location('energy_assets',ROOT/'tools/generate_energy_node_assets.py')
        mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
        paths=[SHARED_ASSETS/f'blockstates/battery_box_{t}.json' for t in TIERS]+list((SHARED_ASSETS/'models/block/machine/energy').glob('battery_box*.json'))+[ASSETS/'lang/en_us.json',ASSETS/'lang/zh_cn.json']
        before={p:hashlib.sha256(p.read_bytes()).digest() for p in paths}
        mod.main(battery_only=True)
        self.assertEqual(before,{p:hashlib.sha256(p.read_bytes()).digest() for p in paths})
if __name__=='__main__':unittest.main()
