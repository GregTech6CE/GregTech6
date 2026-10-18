"""Actual resource graph, tint layers and original GUI provenance."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import hashlib
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from restore_multiblock_appearance import ASSETS, MAPPING, ORIGINAL
from check_render_resources import check


class MachineAppearanceTests(unittest.TestCase):
    def test_all_multiblock_resource_graphs(self):
        issues, _ = check([ASSETS / f'blockstates/{name}.json' for name in MAPPING])
        self.assertEqual([], issues)

    def test_multiblock_layers_and_item_parent(self):
        for name in MAPPING:
            with self.subTest(name=name):
                ref = f'gt6ce:block/machine/multiblock/{name}'
                item = json.loads((ASSETS / f'models/item/{name}.json').read_text())
                self.assertEqual(ref, item['parent'])
                model = json.loads((ASSETS / f'models/block/machine/multiblock/{name}.json').read_text())
                self.assertEqual('forge:composite', model['loader'])
                faces = model['children']['colored']['elements'][0]['faces']
                self.assertEqual(6, len(faces))
                self.assertTrue(all(face['tintindex'] == 0 for face in faces.values()))
                for element in model['children'].get('overlay', {}).get('elements', []):
                    self.assertTrue(all('tintindex' not in face for face in element['faces'].values()))

    def test_machine_guis_are_original_bytes(self):
        originals = list((native_path(ORIGINAL / 'src/main/resources/assets/gregtech/textures/gui/machines')).glob('*.png'))
        self.assertEqual(87, len(originals))
        for source in originals:
            target = ASSETS / 'textures/gui/recipes' / source.name.lower()
            self.assertEqual(hashlib.sha256(source.read_bytes()).digest(), hashlib.sha256(target.read_bytes()).digest(), source.name)

    def test_restored_stone_and_ice_shared_models(self):
        paths = []
        for family in ('stone', 'cube_shiny'):
            models = list((ASSETS / 'models/item/material' / family).glob('*.json'))
            self.assertTrue(models, family)
            paths.extend(models)
        issues, _ = check(paths)
        self.assertEqual([], issues)

    def test_mortar_variant_resources_and_separate_tint(self):
        issues, _ = check(list((ASSETS / 'blockstates').glob('mortar_*.json')))
        self.assertEqual([], issues)
        model = json.loads((ASSETS / 'models/block/machine/tool/mortar_block.json').read_text())
        elements = model['children']['colored']['elements']
        self.assertTrue(all(f['tintindex'] == 1 for f in elements[-1]['faces'].values()))
        self.assertTrue(all(f['tintindex'] == 0 for part in elements[:-1] for f in part['faces'].values()))

    def test_energy_labels_accept_actual_unit(self):
        for locale in ('en_us', 'zh_cn'):
            lang = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
            for key in ('energy_used', 'energy_produced', 'energy_total', 'costs', 'gain', 'usage', 'output', 'tier'):
                text = lang['gt6ce.jei.' + key]
                self.assertNotIn('GU', text)
                self.assertEqual(2, text.count('%s'))


if __name__ == '__main__':
    unittest.main()
