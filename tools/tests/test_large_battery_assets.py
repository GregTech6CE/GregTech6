"""Bounded original electrical-dependency and shared recipe checks."""
import json
import re
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
TIERS=['ulv','lv','mv','hv','ev','iv','luv','zpm','uv','xv']
class LargeBatteryAssets(unittest.TestCase):
    def test_original_grade_wire_and_missing_transformer_facts(self):
        source=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java'
        mt=(source/'gregapi/data/MT.java').read_text(encoding='utf-8')
        for name,size in [('WIRES_01','01'),('WIRES_04','04'),('CABLES_01','01'),('CABLES_04','04')]:
            body=re.search(name+r' = \{(.*?)\n\s*}',mt,re.S).group(1)
            entries=re.findall(r'OP\.(\w+)\.dat\(([^)]+)\)',body)
            self.assertEqual(len(entries),16)
            self.assertEqual(entries[6:11],[('wireGt'+size,'Graphene')]*5)
            self.assertEqual(entries[11:],[('wireGt'+size,'Superconductor')]*5)
        loader=(source/'gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
        self.assertIn("'X', OD_CIRCUITS[i]",loader)
        self.assertIn("'M', aRegistry.getItem(10040+i)",loader)
        self.assertNotRegex(loader,r',\s*10049\s*,')
        items=(source/'gregtech/items/MultiItemTechnological.java').read_text(encoding='utf-8')
        self.assertNotRegex(items,r'addItem\(3030[789]')
    def test_twenty_eight_patterns_and_transformer_dependency(self):
        recipes=list((ROOT/'core/src/main/resources/data/gt6ce/recipes/energy_nodes').glob('*.json'))
        self.assertEqual(len(recipes),28)
        for p in recipes:
            r=json.loads(p.read_text(encoding='utf-8'))
            self.assertEqual(r['pattern'],['WIW','XMx','WIW'] if p.stem.startswith('transformer_') else ['WCW','WCW','XMX'])
            if p.stem.startswith('energy_storage_'):self.assertTrue(r['key']['M']['item'].startswith('gt6ce:transformer_'))
    def test_cumulative_tags_and_generator_idempotence(self):
        import importlib.util
        data=ROOT/'core/src/main/resources/data/gt6ce'
        tags={i:json.loads((data/f'tags/items/circuits_tier_{i}_plus.json').read_text()) for i in range(10)}
        for i,tag in tags.items():
            self.assertFalse(tag['replace'])
            self.assertIn({'id':f'#gt:circuit{i}','required':False},tag['values'])
            self.assertEqual(f'#gt6ce:circuits_tier_{i+1}_plus' in tag['values'],i<9)
            self.assertFalse(any(isinstance(v,str) and 'quantum' in v for v in tag['values']))
        paths=list((data/'recipes/energy_nodes').glob('*.json'))+list((data/'tags/items').glob('circuits_tier_*.json'))
        before={p:p.read_bytes() for p in paths}
        spec=importlib.util.spec_from_file_location('electrical_generator',ROOT/'tools/generate_battery_box_recipes.py')
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.run()
        self.assertEqual(before,{p:p.read_bytes() for p in paths})
        self.assertFalse((data/'recipes/energy_nodes/energy_storage_xv.json').exists())
if __name__=='__main__':unittest.main()
