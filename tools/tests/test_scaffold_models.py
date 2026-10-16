"""Scaffold block states must select the actual structural design."""
import json
from pathlib import Path
import unittest
ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gregtech'
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
class ScaffoldModelTests(unittest.TestCase):
    def test_all_structural_designs(self):
        variants = json.loads((SHARED_ASSETS / 'blockstates/scaffold.json').read_text())['variants']
        self.assertEqual(16, len(variants))
        for design, count in ((0, 4), (1, 7), (2, 6), (3, 1)):
            for facing in ('north', 'east', 'south', 'west'):
                variant = variants[f'design={design},facing={facing}']
                model = json.loads((ASSETS / ('models/' + variant['model'].split(':')[1] + '.json')).read_text())
                model = model.get('children', {}).get('base', model)
                self.assertEqual(count, len(model['elements']))
                if design == 2:
                    self.assertFalse(any(e['from'][0] == 0 and e['to'][0] == 16 and e['from'][2] == 0 and e['to'][2] == 16 for e in model['elements']), 'open scaffold must not render a solid hatch')
                for element in model['elements']:
                    for face in element['faces'].values():
                        self.assertIn(face['texture'][1:], model['textures'])
    def test_original_plate_rod_and_hatch_layers(self):
        for design in range(4):
            suffix = '_' + str(design) if design else ''
            model = json.loads((SHARED_ASSETS / f'models/block/tool/scaffold{suffix}.json').read_text())
            base = model.get('children', {}).get('base', model)
            rod_indices = {0: {1, 2, 3}, 1: {1, 2, 3, 4}, 2: {0, 1, 2, 3}, 3: set()}[design]
            for i, element in enumerate(base['elements']):
                for face in element['faces'].values():
                    self.assertEqual('#rod' if i in rod_indices else '#plate', face['texture'])
                    self.assertEqual(0, face['tintindex'])
            if design == 1:
                hatch = model['children']['hatch']
                self.assertEqual('minecraft:cutout', hatch['render_type'])
                self.assertEqual(3, len(hatch['elements']))
                for element in hatch['elements']:
                    self.assertEqual({'up', 'down'}, set(element['faces']))
                    for face in element['faces'].values():
                        self.assertEqual('#hatch', face['texture'])
                        self.assertEqual(0, face['tintindex'])
            else:
                self.assertNotIn('hatch', model.get('children', {}))

    def test_item_uses_standing_hatch(self):
        item = json.loads((SHARED_ASSETS / 'models/item/scaffold.json').read_text())
        self.assertEqual('gregtech:block/tool/scaffold_1', item['parent'])
if __name__ == '__main__':
    unittest.main()
