"""GT6 piston plates use front/back by face, never on their top/bottom edges."""
import json
from pathlib import Path
import unittest

ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gregtech'
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"


class PistonModelTests(unittest.TestCase):
    def test_plate_face_roles_and_separate_core_tint(self):
        for kind in ('electric', 'flux'):
            model = json.loads((SHARED_ASSETS / f'models/block/machine/engine/engine_{kind}.json').read_text(encoding='utf-8'))
            for name, child in model['children'].items():
                for element in child['elements']:
                    for direction, face in element['faces'].items():
                        if 'plate_' in name:
                            expected = '#front' if direction == 'north' else '#back' if direction == 'south' else '#side'
                            self.assertEqual(expected, face['texture'], (kind, name, direction))
                        if name == 'engine_core_colored':
                            self.assertEqual(1, face['tintindex'])
                        if name.endswith('_overlay'):
                            self.assertNotIn('tintindex', face)


if __name__ == '__main__':
    unittest.main()
