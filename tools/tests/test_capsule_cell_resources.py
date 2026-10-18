
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gregtech')
sys.path.insert(0, str(ROOT / 'tools'))
from check_render_resources import check


class CapsuleCellResources(unittest.TestCase):
    def test_all_source_variants_have_models_and_languages(self):
        rows = json.loads((ROOT / 'docs/capsule-cell-source.json').read_text(encoding='utf-8'))
        self.assertEqual(len({r['id'] for r in rows}), 40)
        paths = []
        for row in rows:
            paths.extend([ASSETS / f"blockstates/fluid_{row['id']}.json", ASSETS / f"models/item/fluid_{row['id']}.json"])
        issues, _ = check(paths)
        self.assertFalse(issues)
        for locale in ['en_us', 'zh_cn']:
            lang = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
            for row in rows:
                self.assertIn('block.gregtech.fluid_' + row['id'], lang)

    def test_all_eight_original_textures_keep_source_bytes(self):
        ledger = json.loads((ROOT / 'tools/gt6_texture_sources.json').read_text(encoding='utf-8'))
        entries = {p: value for p, value in ledger.items() if p.startswith('textures/block/machines/tanks/cell/')}
        self.assertEqual(len(entries), 8)
        for path, entry in entries.items():
            self.assertEqual(hashlib.sha256((ASSETS / path).read_bytes()).hexdigest(), entry['sha256'])

    def test_core_and_geiger_source_components(self):
        recipes = native_path(ROOT / 'src/main/resources/data/gregtech/recipes/nuclear')
        core = json.loads((recipes / 'reactor_core_2x2.json').read_text(encoding='utf-8'))
        self.assertEqual(core['key']['M']['item'], 'gregtech:casing_machine_dense_lead')
        geiger = json.loads((recipes / 'geiger_counter_empty.json').read_text(encoding='utf-8'))
        self.assertTrue(geiger['require_empty_fluid_containers'])
        for recipe in [core, geiger]:
            self.assertEqual(set(''.join(recipe['pattern'])) - {' '}, set(recipe['key']))
