import json
import unittest
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gregtech'
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"


class InventoryRootTests(unittest.TestCase):
    def test_burning_boxes_have_top_faces_in_all_registered_variants(self):
        for directory in ('burning_liquid', 'burning_gas', 'burning_fluidbed'):
            for direction in ('north', 'east', 'south', 'west'):
                for active in ('on', 'off'):
                    path = SHARED_ASSETS / f'models/block/machine/{directory}/{direction}_{active}.json'
                    model = json.loads(path.read_text(encoding='utf-8'))
                    base = model['children']['layer0']
                    tops = [part['faces']['up'] for part in base['elements'] if 'up' in part['faces']]
                    self.assertTrue(tops, str(path))
                    for face in tops:
                        texture = base['textures'][face['texture'].removeprefix('#')]
                        self.assertTrue((ASSETS / ('textures/' + texture.split(':')[1] + '.png')).is_file(), texture)

    def test_composite_inventory_roots_inherit_block_transforms(self):
        for name in ('machine/energy/reactor_casing', 'machine/tank_barrel',
                     'machine/burning_liquid/north_off', 'machine/burning_fluidbed/north_off'):
            model = json.loads((SHARED_ASSETS / f'models/block/{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(model.get('parent'), 'minecraft:block/block', name)

    def test_axle_sprite_is_available_in_dynamic_block_atlas(self):
        atlas = json.loads((ASSETS.parent / 'minecraft/atlases/blocks.json').read_text(encoding='utf-8'))
        self.assertTrue(any(source.get('resource') == 'gregtech:block/iconsets/axle'
                            or (source.get('source') == 'block/iconsets') for source in atlas['sources']))


if __name__ == '__main__':
    unittest.main()
