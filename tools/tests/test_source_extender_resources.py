import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
sys.path.insert(0, str(ROOT / 'tools'))
from rebuild_source_extenders import SPECS, DIRECTIONS
from check_render_resources import check


class SourceExtenderResources(unittest.TestCase):
    def test_all_faces_and_secondary_faces_have_source_models(self):
        paths = []
        for name, texture, _ in SPECS:
            path = SHARED_ASSETS / f'blockstates/{name}.json'
            paths.extend([path, SHARED_ASSETS / f'models/item/{name}.json'])
            data = json.loads(path.read_text(encoding='utf-8'))
            if not texture.startswith('bridge_'):
                self.assertEqual(set(data['variants']), {f'facing={a},secondary={b}' for a in DIRECTIONS for b in DIRECTIONS})
                for a in DIRECTIONS:
                    for b in DIRECTIONS:
                        model = json.loads((SHARED_ASSETS / f'models/block/logistics/{name}/{a}_{b}.json').read_text(encoding='utf-8'))
                        for layer, child in model['children'].items():
                            faces = child['elements'][0]['faces']
                            self.assertEqual(faces[a]['texture'], '#in')
                            if a != b:
                                self.assertEqual(faces[b]['texture'], '#out')
                            for side in DIRECTIONS:
                                self.assertEqual(faces[side].get('tintindex'), 0 if layer == 'colored' else None)
        issues, count = check(paths)
        self.assertFalse(issues)
        self.assertGreaterEqual(count, 123)

    def test_original_texture_hashes(self):
        ledger = json.loads((ROOT / 'tools/gt6_texture_sources.json').read_text(encoding='utf-8'))
        for _, texture, _ in SPECS:
            faces = ['side'] if texture.startswith('bridge_') else ['in', 'out', 'side']
            for layer in ['colored', 'overlay']:
                for face in faces:
                    path = f'textures/block/machines/extenders/{texture}/{layer}/{face}.png'
                    self.assertEqual(hashlib.sha256((ASSETS / path).read_bytes()).hexdigest(), ledger[path]['sha256'])

    def test_recipes_and_bilingual_names(self):
        for locale in ['zh_cn', 'en_us']:
            lang = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
            for name, _, _ in SPECS:
                self.assertIn('block.gregtech.' + name, lang)
        for name, _, pattern in SPECS:
            recipe = json.loads((ROOT / f'src/main/resources/data/gregtech/recipes/logistics/{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(recipe['pattern'], pattern)
            self.assertEqual(set(recipe['key']), set(''.join(pattern)) - {' '})
            self.assertEqual(recipe['type'], 'gregtech:tool_shaped')

    def test_capsule_source_alias_and_forming_groups(self):
        rows = json.loads((ROOT / 'docs/capsule-cell-source.json').read_text(encoding='utf-8'))
        self.assertEqual(next(r for r in rows if r['original'] == 32616)['material'], 'Trinium')
        self.assertEqual(sum(r['extrusion'] == 'simple' for r in rows), 12)
        self.assertEqual(sum(r['extrusion'] == 'hot' for r in rows), 28)
