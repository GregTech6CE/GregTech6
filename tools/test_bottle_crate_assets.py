import json
import tempfile
import unittest
from pathlib import Path

from generate_bottle_crate_assets import generate, ORIGINAL


class BottleCrateAssetsTest(unittest.TestCase):
    def test_original_frame_geometry_and_direction_states(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generate(root)
            model = json.loads((root / 'models/block/inventory/bottle_crate.json').read_text())
            boxes = [(e['from'], e['to']) for e in model['elements']]
            self.assertEqual(len(boxes), 9)
            self.assertEqual(boxes[0], ([1, 0, 1], [15, 1, 15]))
            self.assertEqual(boxes[1], ([1, 1, 5], [15, 5, 6]))
            self.assertEqual(boxes[3], ([5, 1, 1], [6, 4, 15]))
            self.assertEqual(boxes[5], ([0, 0, 0], [1, 8, 16]))
            self.assertEqual(boxes[7], ([1, 5, 0], [15, 7, 1]))
            self.assertEqual(model['textures']['wood'], 'gt6ce:block/iconsets/planks_treated')
            self.assertFalse(model['ambientocclusion'])
            states = json.loads((root / 'blockstates/bottle_crate.json').read_text())['variants']
            self.assertEqual([states['facing='+f]['y'] for f in ('north','south','east','west')], [0,0,90,90])
            for element in model['elements']:
                self.assertEqual(len(element['faces']), 6)
                self.assertTrue(all(f['texture'] == '#wood' for f in element['faces'].values()))

    def test_four_textures_are_exact_original_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generate(root)
            for name in ('BOTTLECRATE_BOTTLE_TOP','BOTTLECRATE_BOTTLE_SIDES','BOTTLECRATE_BOTTLE_CAP','PLANKS_TREATED'):
                actual = (root / f'textures/block/iconsets/{name.lower()}.png').read_bytes()
                expected = (ORIGINAL / f'textures/blocks/iconsets/{name}.png').read_bytes()
                self.assertEqual(actual, expected)
                self.assertTrue(actual.startswith(b'\x89PNG\r\n\x1a\n'))


if __name__ == '__main__':
    unittest.main()
