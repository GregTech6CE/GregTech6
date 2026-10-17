"""A placed button must sit against the supporting block, opposite its outward face."""
import itertools
import json
import math
from pathlib import Path
import unittest

ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gregtech'
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"

class AdvancedButtonModelTests(unittest.TestCase):
    def test_legacy_regeneration_preserves_specialized_states(self):
        import sys
        import tempfile
        from unittest.mock import patch
        sys.path.insert(0, str(ASSETS.parents[4] / 'tools'))
        import rebuild_container_tool_models as legacy
        import generate_advanced_button_assets as button
        import generate_scaffold_models as scaffold
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            assets = root / 'src/main/resources/assets/gregtech'
            with patch.object(legacy, 'ROOT', root), patch.object(legacy, 'ASSETS', assets), \
                    patch.object(legacy, 'SHARED_ASSETS', assets), \
                    patch.object(button, 'ASSETS', assets), patch.object(button, 'SHARED_ASSETS', assets), \
                    patch.object(button, 'TARGET', assets / 'textures/block/machines/redstone/buttons/advanced/0/0'), \
                    patch.object(scaffold, 'ROOT', assets):
                legacy.main()
                legacy.main()
            for block in ('advanced_button', 'scaffold'):
                generated = json.loads((assets / f'blockstates/{block}.json').read_text())
                shipped = json.loads((SHARED_ASSETS / f'blockstates/{block}.json').read_text())
                self.assertEqual(shipped, generated, block)

    def test_six_faces_touch_support(self):
        variants = json.loads((SHARED_ASSETS / 'blockstates/advanced_button.json').read_text())['variants']
        self.assertEqual(24, len(variants), 'all six faces and both active/lit states must exist')
        directions = {'east': (0, 1), 'west': (0, -1), 'up': (1, 1),
                      'down': (1, -1), 'south': (2, 1), 'north': (2, -1)}
        for key, variant in variants.items():
            facing = dict(part.split('=') for part in key.split(','))['facing']
            axis, sign = directions[facing]
            model = json.loads((ASSETS / ('models/' + variant['model'].split(':')[1] + '.json')).read_text())
            for name, child in model.get('children', {'legacy': model}).items():
                element = child['elements'][0]
                points = []
                for point in itertools.product(*zip(element['from'], element['to'])):
                    x, y, z = (v - 8 for v in point)
                    a = math.radians(-variant.get('x', 0))
                    y, z = y*math.cos(a)-z*math.sin(a), y*math.sin(a)+z*math.cos(a)
                    a = math.radians(-variant.get('y', 0))
                    x, z = x*math.cos(a)+z*math.sin(a), -x*math.sin(a)+z*math.cos(a)
                    points.append((x+8, y+8, z+8))
                coordinates = [point[axis] for point in points]
                expected = (0, 2) if sign > 0 else (14, 16)
                self.assertAlmostEqual(expected[0], min(coordinates), msg=(key, name))
                self.assertAlmostEqual(expected[1], max(coordinates), msg=(key, name))

if __name__ == '__main__':
    unittest.main()
