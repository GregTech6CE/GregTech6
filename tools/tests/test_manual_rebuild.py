import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'

CORE_ASSETS = ROOT / 'core/src/main/resources/assets/gregtech'

def read(path):
    relative = Path(path)
    for root in (SHARED_ASSETS, CORE_ASSETS, ASSETS):
        candidate = root / relative
        if candidate.is_file():
            return json.loads(candidate.read_text(encoding='utf-8'))
    raise FileNotFoundError(path)

class ManualRebuildTests(unittest.TestCase):
    def test_original_mortar_is_hollow_with_pestle(self):
        parts = read('models/block/machine/tool/mortar_block.json')['children']['colored']['elements']
        self.assertIn(([6,0,6], [10,9,10]), [(p['from'],p['to']) for p in parts])
        # A point inside the bowl, away from its pestle, must remain empty.
        for part in parts:
            self.assertFalse(all(a < value < b for a,value,b in zip(part['from'], [5,4,5], part['to'])))

    def test_sifter_has_lower_tray_and_original_legs(self):
        parts = read('models/block/machine/tool/sifting_table.json')['children']['colored']['elements']
        bounds = [(p['from'],p['to']) for p in parts]
        self.assertIn(([0,2,0],[16,5,16]), bounds)
        self.assertIn(([0,0,0],[2,13,2]), bounds)
        self.assertIn(([14,0,14],[16,13,16]), bounds)

    def test_grindstone_variants_include_empty_and_all_horizontal_facings(self):
        variants = read('blockstates/grindstone_block.json')['variants']
        self.assertEqual(8, len(variants))
        for face in ('north','south','west','east'):
            self.assertTrue(variants[f'facing={face},stone=false']['model'].endswith('grindstone_empty'))

    def test_all_manual_layers_resolve_textures(self):
        for name in ('mortar_block','grindstone_block','grindstone_empty','sifting_table','crank'):
            model = read(f'models/block/machine/tool/{name}.json')
            self.assertEqual({'colored','overlay'}, set(model['children']))
            for layer, child in model['children'].items():
                for texture in child['textures'].values():
                    self.assertTrue((ASSETS/'textures'/(texture.split(':')[1]+'.png')).is_file(), texture)
                if layer == 'overlay':
                    self.assertTrue(all('tintindex' not in face for part in child['elements'] for face in part['faces'].values()))

    def test_chest_items_use_shared_custom_mesh(self):
        self.assertEqual('builtin/entity', read('models/item/metal_chest.json')['parent'])
        for path in (SHARED_ASSETS / 'models/item').glob('chest_*.json'):
            self.assertEqual('gregtech:item/metal_chest', json.loads(path.read_text(encoding='utf-8'))['parent'])

if __name__ == '__main__': unittest.main()
