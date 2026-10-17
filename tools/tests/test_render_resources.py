"""Regression checks on the actual controller and six-direction panel resources."""
import json
from pathlib import Path
import unittest
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gregtech'
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"


class RenderResourceTests(unittest.TestCase):
    def test_decoration_models_reference_existing_textures(self):
        from check_render_resources import check
        issues, _ = check((SHARED_ASSETS / 'models/block/decoration').glob('*.json'))
        self.assertEqual([], issues)

    def test_reachable_controller_and_panel_resources(self):
        from check_render_resources import check, CONTROLLERS
        paths = list((SHARED_ASSETS / 'blockstates').glob('panel_*.json'))
        paths += [SHARED_ASSETS / f'blockstates/{name}.json' for name in CONTROLLERS]
        issues, _ = check(paths)
        self.assertEqual([], issues)

    def test_controller_parents_exist(self):
        for name in ('bedrock_drill_main', 'fusion_reactor_main', 'implosion_compressor_main', 'lightning_rod_main'):
            model = json.loads((SHARED_ASSETS / f'models/block/machine/{name}.json').read_text())
            parent = model['parent']
            if parent.startswith('gregtech:'):
                self.assertTrue((SHARED_ASSETS / f'models/{parent.split(":")[1]}.json').is_file(), name)
            else:
                self.assertEqual('minecraft:block/orientable', parent)
            for texture in model['textures'].values():
                self.assertTrue((ASSETS / f'textures/{texture.split(":")[1]}.png').is_file(), texture)

    def test_panels_cover_all_placeable_faces(self):
        for path in (SHARED_ASSETS / 'blockstates').glob('panel_*.json'):
            variants = json.loads(path.read_text())['variants']
            with self.subTest(panel=path.name):
                self.assertEqual({f'facing={face}' for face in ('up', 'down', 'north', 'south', 'east', 'west')}, set(variants))
                self.assertEqual(90, variants['facing=down']['x'])
                self.assertEqual(270, variants['facing=up']['x'])


if __name__ == '__main__':
    unittest.main()
