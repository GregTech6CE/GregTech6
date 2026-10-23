"""Issue #76: verify shipped/generated alpha-cutout saplings, not rendered client pixels."""
import contextlib
import io
import json
from pathlib import Path
import runpy
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'core/src/main/resources/assets/gt6ce'


class SaplingTransparencyTests(unittest.TestCase):
    def test_every_shipped_sapling_is_cutout_and_resolves_both_growth_stages(self):
        models = sorted((ASSETS / 'models/block/wood').glob('sapling_*.json'))
        self.assertEqual(12, len(models))
        for path in models:
            with self.subTest(model=path.name):
                model = json.loads(path.read_text())
                self.assertEqual('minecraft:block/cross', model['parent'])
                self.assertEqual('minecraft:cutout', model['render_type'])
                namespace, texture = model['textures']['cross'].split(':')
                if namespace == 'gt6ce':
                    self.assertTrue((ASSETS / 'textures' / (texture + '.png')).is_file())
                states = json.loads((ASSETS / 'blockstates' / path.name).read_text())['variants']
                for stage in (0, 1):
                    self.assertEqual('gt6ce:block/wood/' + path.stem, states[f'stage={stage}']['model'])

    def test_generator_preserves_cutout_and_existing_texture_aliases(self):
        # The generator executes at import time: redirect its native path before execution.
        with tempfile.TemporaryDirectory() as directory:
            native = types.ModuleType('native_layout')
            native.native_path = lambda part: Path(directory) / part
            with patch.dict(sys.modules, {'native_layout': native}), contextlib.redirect_stdout(io.StringIO()):
                runpy.run_path(str(ROOT / 'tools/generate_gt6_tree_assets.py'))
            generated = Path(directory) / 'src/main/resources/assets/gt6ce/models/block/wood'
            for path in (ASSETS / 'models/block/wood').glob('sapling_*.json'):
                self.assertEqual(json.loads(path.read_text()), json.loads((generated / path.name).read_text()), path.name)


if __name__ == '__main__':
    unittest.main()
