import json
import unittest
from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
from PIL import Image
from tools.check_render_resources import check
from tools.restore_laser_models import ASSETS, FAMILIES
from tools.generate_anvil_materials import names, SOURCE


class OpticalAndReactorResources(unittest.TestCase):
    def test_optical_models_resolve_for_every_state(self):
        paths = [SHARED_ASSETS / f'blockstates/{family}_{tier}.json'
                 for family, (_, tiers) in FAMILIES.items() for tier in tiers]
        paths.append(SHARED_ASSETS / 'blockstates/laser_fiber_wire.json')
        issues, _ = check(paths)
        self.assertEqual([], issues)
        for path in paths[:-1]:
            model = json.loads(path.read_text(encoding='utf8'))
            self.assertEqual(12, len(model['variants']))
            self.assertEqual(2, len({value['model'] for value in model['variants'].values()}))

    def test_reactor_windows_use_original_alpha_and_correct_variant(self):
        for name, folder in [('reactor_core', 'reactor_core_1x1'), ('reactor_core_2x2', 'reactor_core_2x2')]:
            model = json.loads((SHARED_ASSETS / f'models/block/machine/energy/{name}.json').read_text(encoding='utf8'))
            for child in model['children'].values():
                self.assertEqual('minecraft:cutout', child['render_type'])
                self.assertTrue(all(folder in texture for texture in child['textures'].values()))
            path = ASSETS / f'textures/block/machines/generators/{folder}/colored/side1.png'
            with Image.open(path) as image:
                self.assertEqual((0, 255), image.convert('RGBA').getchannel('A').getextrema())
            issues, _ = check([SHARED_ASSETS / f'blockstates/{name}.json'])
            self.assertEqual([], issues)

    def test_forging_catalog_includes_factory_inheritance_and_excludes_coating(self):
        imported = set(names(SOURCE.read_text(encoding='utf8')))
        self.assertTrue({'Iron', 'Copper', 'Nickel', 'Bronze', 'Steel'} <= imported)
        self.assertFalse({'GalvanizedSteel', 'FrozenIron', 'TungstenSintered', 'Graphene'} & imported)


if __name__ == '__main__':
    unittest.main()
