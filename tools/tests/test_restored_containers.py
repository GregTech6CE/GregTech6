import hashlib
import json
import sys
import unittest
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS_DIR))
from rebuild_container_tool_models import ASSETS, TOOLS
from check_render_resources import check


def read(path):
    return json.loads((ASSETS / path).read_text(encoding='utf-8'))


class RestoredContainerTests(unittest.TestCase):
    def test_open_vessel_cavities(self):
        for name, point in [('cup', (8, 3, 8)), ('measuring_pot', (8, 5, 8)), ('jug', (8, 12, 8))]:
            model = read(f'models/block/machine/container/{name}.json')
            for child in model['children'].values():
                self.assertFalse(any(all(a < x < b for a, x, b in zip(part['from'], point, part['to']))
                                     for part in child['elements']), name)
            self.assertEqual(read(f'models/item/fluid_{name}.json')['parent'],
                             f'gt6ce:block/machine/container/{name}')

    def test_tool_facing_resources(self):
        for name in TOOLS:
            variants = read(f'blockstates/{name}.json')['variants']
            expected = {'facing=north', 'facing=east', 'facing=south', 'facing=west'}
            if name in {'tap', 'fluid_funnel', 'cap_nozzle'}:
                expected.add('facing=down')  # Shared attachment state; only funnels permit this via placement/wrench.
            self.assertEqual(set(variants), expected)
            self.assertEqual([variants[f'facing={f}']['y'] for f in ('north', 'east', 'south', 'west')],
                             [0, 90, 180, 270])
            issues, _ = check([ASSETS / f'blockstates/{name}.json'])
            self.assertEqual(issues, [])

    def test_original_assets_preserved(self):
        manifest = json.loads((TOOLS_DIR / 'gt6_texture_sources.json').read_text(encoding='utf-8'))
        self.assertGreater(len(manifest), 0)
        for relative, record in manifest.items():
            self.assertEqual(hashlib.sha256((ASSETS / relative).read_bytes()).hexdigest(), record['sha256'], relative)

    def test_rod_is_narrow_three_dimensional_mesh(self):
        model = read('models/block/machine/container/reactor_rod.json')
        part = model['children']['colored']['elements'][0]
        self.assertEqual(part['from'], [6, 0, 6])
        self.assertEqual(part['to'], [10, 16, 10])
        for face in part['faces'].values():
            self.assertEqual(face['tintindex'], 0)
        for part in model['children']['overlay']['elements']:
            self.assertTrue(all('tintindex' not in face for face in part['faces'].values()))


if __name__ == '__main__':
    unittest.main()
