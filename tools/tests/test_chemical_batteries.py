
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import hashlib
import importlib.util
import json
import re
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ASSETS=native_path(ROOT/'src/main/resources/assets/gt6ce')
DATA=native_path(ROOT/'src/main/resources/data/gt6ce')
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master/src/main'
CHEM=['lead_acid','alkaline','nickel_cadmium','lithium_cobalt','lithium_manganese']
TIERS=['ulv','lv','mv','hv','ev']

class ChemicalBatteryResources(unittest.TestCase):
    def test_forty_textures_identical_to_gt6(self):
        for kind in ['standard','advanced']:
            for voltage in [8,32,128,512,2048]:
                for face in ['bottom','top','sides','bar']:
                    original=ORIGINAL/f'resources/assets/gregtech/textures/blocks/machines/batteries/eu/{kind}/{voltage}/{face}.png'
                    port=ASSETS/f'textures/block/machines/batteries/eu/{kind}/{voltage}/{face}.png'
                    self.assertEqual(port.read_bytes(),original.read_bytes())

    def test_charged_inventory_models_bounds_and_predicates(self):
        for c,chem in enumerate(CHEM):
            for t,tier in enumerate(TIERS):
                ident=f'battery_{chem}_{tier}'
                model=json.loads((ASSETS/f'models/item/{ident}.json').read_text())
                self.assertEqual(len(model['overrides']),[4,7,7,7,9][t])
                self.assertEqual(model['overrides'][-1]['predicate']['gt6ce:battery_charge'],1)
                for override in model['overrides']:
                    fraction=override['predicate']['gt6ce:battery_charge']
                    self.assertTrue(0<fraction<=1,'Minecraft clamps property functions to 0..1')
                    path=ASSETS/('models/'+override['model'].split(':')[1]+'.json')
                    charged=json.loads(path.read_text())
                    body,bar=charged['elements']
                    inset=[5,5,4,3,2][t]
                    self.assertEqual(body['from'],[inset,0,inset])
                    self.assertEqual(body['to'],[16-inset,[8,11,11,11,13][t],16-inset])
                    self.assertEqual(set(bar['faces']),{'north','south','west','east'})
                    self.assertTrue(all(f['tintindex']==0 for f in bar['faces'].values()))
                state=json.loads((ASSETS/f'blockstates/{ident}.json').read_text())
                self.assertEqual(state['variants']['']['model'],model['parent'])

    def test_original_recipes_standard_chinese_and_circuit_substitution(self):
        source=(ORIGINAL/'java/gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
        cn=(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8')
        lang=json.loads((ASSETS/'lang/zh_cn.json').read_text(encoding='utf-8'))
        for c,chem in enumerate(CHEM):
            for t,tier in enumerate(TIERS):
                number=14000+c*10+t;ident=f'battery_{chem}_{tier}'
                line=next(line for line in source.splitlines() if re.search(r',\s*'+str(number)+r'\s*,\s*14013',line))
                expected=[s for s in re.findall(r'"([^"]*)"',line) if re.fullmatch(r'[ WxPBC]+',s)]
                recipe=json.loads((DATA/f'recipes/chemical_batteries/{ident}.json').read_text())
                self.assertEqual(recipe['pattern'],expected)
                self.assertEqual(set(recipe['key']),set(''.join(expected)) - {' '})
                self.assertFalse(recipe['allow_mirror'])
                self.assertEqual(recipe['result']['item'],'gt6ce:'+ident)
                self.assertEqual(lang['block.gt6ce.'+ident],re.search(r'S:gt\.multitileentity\.'+str(number)+r'=([^\r\n]+)',cn).group(1))
                if 'C' in recipe['key']:
                    circuit=int(re.search(r"'C', OD_CIRCUITS\[(\d+)\]",line).group(1))
                    self.assertEqual(recipe['key']['C']['tag'],f'gt6ce:circuits_tier_{circuit}_plus')
        values=json.loads((DATA/'tags/items/circuits_tier_2_plus.json').read_text())['values']
        self.assertIn('gt6ce:circuit_good',values)
        self.assertIn('gt6ce:circuit_ultimate',values)
        self.assertNotIn('gt6ce:circuit_basic',values)

    def test_generator_is_idempotent(self):
        paths=list((ASSETS/'models/block/chemical_battery').glob('*.json'))+list((ASSETS/'models/item').glob('battery_*_*.json'))+list((DATA/'recipes/chemical_batteries').glob('*.json'))+[ASSETS/'lang/en_us.json',ASSETS/'lang/zh_cn.json']
        before={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
        spec=importlib.util.spec_from_file_location('battery_generator',ROOT/'tools/generate_chemical_batteries.py')
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.run()
        self.assertEqual(before,{p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths})

if __name__=='__main__':unittest.main()
