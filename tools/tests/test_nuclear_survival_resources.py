
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gt6ce')
sys.path.insert(0, str(ROOT / 'tools'))
from check_render_resources import check


class NuclearSurvivalResources(unittest.TestCase):
    def test_placed_rods_and_suit_resolve_real_assets(self):
        rods = json.loads((ROOT / 'docs/reactor-rod-source.json').read_text(encoding='utf-8'))
        paths = [ASSETS / f"blockstates/{r['id']}.json" for r in rods]
        for part in ['helmet', 'chestplate', 'leggings', 'boots']:
            paths.append(ASSETS / f'models/item/radiation_suit_{part}.json')
        issues, count = check(paths)
        self.assertFalse(issues)
        self.assertGreater(count, 49)
        for i in range(4):
            self.assertTrue((ASSETS / f'textures/armor/hazard_radiation/{i}.png').is_file())

    def test_funnel_bottom_mesh_matches_gt6(self):
        model = json.loads((ASSETS / 'models/block/tool/fluid_funnel_down.json').read_text(encoding='utf-8'))
        expected = [([5, 2, 5], [11, 3, 11]), ([6, 1, 6], [10, 2, 10]), ([7, 0, 7], [9, 1, 9])]
        for child in model['children'].values():
            self.assertEqual([(e['from'], e['to']) for e in child['elements']], expected)
        for p in (ASSETS / 'blockstates').glob('fluid_funnel*.json'):
            d = json.loads(p.read_text(encoding='utf-8'))
            self.assertEqual(d['variants']['facing=down']['model'], 'gt6ce:block/tool/fluid_funnel_down')
            issues, _ = check([p])
            self.assertFalse(issues)

    def test_suit_recipes_use_exactly_the_declared_pattern_keys(self):
        for p in (native_path(ROOT / 'src/main/resources/data/gt6ce/recipes/nuclear')).glob('radiation_suit_*.json'):
            d = json.loads(p.read_text(encoding='utf-8'))
            symbols = set(''.join(d['pattern'])) - {' '}
            self.assertEqual(symbols, set(d['key']))
            self.assertEqual(d['key']['L'], {'tag': 'forge:plates/lead'})

    def test_new_messages_and_protection_tag(self):
        tag = json.loads((native_path(ROOT / 'src/main/resources/data/gt6ce/tags/items/radiation_protection.json')).read_text(encoding='utf-8'))
        self.assertEqual(len(set(tag['values'])), 4)
        for locale in ['zh_cn', 'en_us']:
            lang = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
            for name in tag['values']:
                self.assertIn('item.' + name.replace(':', '.'), lang)
            for key in ['gt.tooltip.radiation_suit', 'message.gt6ce.geiger.reading',
                        'message.gt6ce.portable_fluid.limit', 'tooltip.gt6ce.portable_fluid.limit_use']:
                self.assertIn(key, lang)
