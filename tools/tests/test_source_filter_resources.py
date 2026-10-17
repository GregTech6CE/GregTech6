import json
import sys
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS=ROOT/'src/main/resources/assets/gregtech'
sys.path.insert(0,str(ROOT/'tools'))
from rebuild_source_filters import FILTERS
from rebuild_source_extenders import DIRECTIONS
from check_render_resources import check


class SourceFilterResources(unittest.TestCase):
    def test_every_facing_and_legacy_state_has_a_model(self):
        paths=[]
        opposite=dict(zip(DIRECTIONS,('up','down','south','north','east','west')))
        for name,texture,_ in FILTERS:
            p=SHARED_ASSETS/f'blockstates/{name}.json';paths.extend([p,SHARED_ASSETS/f'models/item/{name}.json'])
            variants=json.loads(p.read_text(encoding='utf-8'))['variants']
            self.assertEqual(len(variants),72)
            for front in DIRECTIONS:
                for secondary in DIRECTIONS:
                    for explicit in ['false','true']:
                        actual=secondary if explicit=='true' else opposite[front]
                        self.assertEqual(variants[f'facing={front},secondary={secondary},secondary_set={explicit}']['model'],f'gregtech:block/logistics/{texture}/{front}_{actual}')
        paths += [SHARED_ASSETS/f'models/item/{name}.json' for name in ['blank_cover','item_filter','fluid_filter']]
        issues,_=check(paths)
        self.assertFalse(issues)

    def test_recipes_use_actual_casing_ids_and_original_patterns(self):
        folder=ROOT/'src/main/resources/data/gregtech/recipes/logistics'
        for name,_,pattern in FILTERS:
            recipe=json.loads((folder/f'{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(recipe['pattern'],pattern)
            self.assertEqual(set(recipe['key']),set(''.join(pattern))-{' '})
            self.assertTrue(recipe['key']['M']['item'].endswith('_steelgalvanized'))
            reset=json.loads((folder/f'{name}_reset.json').read_text(encoding='utf-8'))
            self.assertEqual(reset['ingredients'],[{'item':'gregtech:'+name}])
            self.assertNotIn('nbt',reset['result'])

    def test_new_filter_messages_are_translated(self):
        for locale in ['en_us','zh_cn']:
            lang=json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8'))
            for key in ['tooltip.gregtech.filter.routing','tooltip.gregtech.filter.tools','tooltip.gregtech.filter.prefix','gregtech.filter.prefix_title']:
                self.assertIn(key,lang)
